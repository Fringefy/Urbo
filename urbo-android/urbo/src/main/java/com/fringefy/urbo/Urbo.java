package com.fringefy.urbo;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Urbo implements LocationListener, GoogleApiClient.ConnectionCallbacks {
	private static final String TAG = "Urbo";

// Inner Types

	public static class Params {
		// location update frequency (milliseconds), see location services docs
		private static final int UPDATE_INTERVAL = 5000;

		// fastest update frequency (milliseconds), see location services docs
		private static final int FASTEST_INTERVAL = 1000;

		public String sEndpoint = "https://odie.fringefy.com/odie";
		public String sApiKey;
		private static final int MAX_ODIE_CONNECTIONS = 2;

		boolean bIdentify = true;
		File fImgDir;

		// constructor should at least provide the ApiKey
		public Params(String sApiKey) {
			this.sApiKey = sApiKey;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null) {
				return false;
			}
			if (other instanceof Params) {
				return equals((Params) other);
			}
			return false;
		}

		private boolean equals(Params other) {
			if (other.bIdentify != bIdentify) {
				return false;
			}
			if (other.sEndpoint == null || !other.sEndpoint.equals(sEndpoint)) {
				return false;
			}
			if (sApiKey != null) {
				if (other.sApiKey == null) {
					return false;
				}
				else if (!other.sApiKey.equals(sApiKey)) {
					return false;
				}
			}
			else {
				if (other.sApiKey != null) {
					return false;
				}
			}
			if (other.fImgDir != null && !other.fImgDir.equals(fImgDir)) {
				return false;
			}
			return true;
		}
	}

// Private Fields

	static private Urbo urbo = null;
	final Params params;

    final String sDeviceId;
	private String sCountryCode; // TODO: it's not initialized today

	private Odie odie;
	private final OdieBlob odieBlob;
	private final ExecutorService xsIo; // TODO: shift executor to C++

	private final LocationManager locationManager;
	private final GoogleApiClient googleApiClient;
	private final FusedLocationProviderApi locationApi;
	private final LocationRequest locationRequest;
	private DebugListener debugListener;


// Construction

	private Urbo(@NonNull Context context, @NonNull Params initParams) {

		params = initParams;
		if (params.fImgDir == null) {
			params.fImgDir = context.getExternalCacheDir();
		}
		if (params.fImgDir == null) {
			params.fImgDir = context.getCacheDir();
		}
		removeOldImageFiles();

		sDeviceId = getDeviceId();

		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		// create the LocationRequest object
		locationRequest = LocationRequest.create();
		// use high accuracy
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// set the update intervals
		locationRequest.setInterval(Params.UPDATE_INTERVAL);
		locationRequest.setFastestInterval(Params.FASTEST_INTERVAL);

		locationApi = LocationServices.FusedLocationApi;

		googleApiClient = new GoogleApiClient.Builder(context)
				.addApi(LocationServices.API)
				.addConnectionCallbacks(this)
				.build();

		// connect to the Google API
		googleApiClient.connect();

		odieBlob = new OdieBlob();
		xsIo = Executors.newFixedThreadPool(Params.MAX_ODIE_CONNECTIONS);

		Pexeso.init(null, this);

		odie = OdieFactory.getInstance(params.sEndpoint, params.sApiKey);
		urbo = this;
	}

	private void removeOldImageFiles() {
		for (File oldImage: new File(params.fImgDir, "").
				listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String filename) {
						return filename.endsWith(".jpg");
					}
				})) {
			Log.w(TAG, "old file " + oldImage.getName() + " " + oldImage.length());
			oldImage.delete();
		}
	}

	/**
	 * This method will create and load Urbo. <br>
	 * @param context The context of application that will use Urbo.
	 * @param initParams  May override some defaults. Note that *params.sApiKey* default is null.
	 */
	public static Urbo createInstance(Context context, Params initParams) {
		if (context == null) {
			throw new IllegalArgumentException("createInstance() requires context");
		}
		if (initParams == null) {
			throw new IllegalArgumentException("createInstance() requires initParams");
		}
		if (urbo != null && urbo.params.equals(initParams)) {
			return urbo;
		}
		if (urbo != null) {
			throw new IllegalStateException("Urbo instance already exists!");
		}
		return (urbo = new Urbo(context, initParams));
	}

	public static Urbo getInstance() {
		return urbo;
	}


// Public Interface

	public static final int STATE_COLD_START = -1;
	public static final int STATE_SEARCH = 0;
	public static final int STATE_RECOGNITION = 1;
	public static final int STATE_NO_RECOGNITION = 2;
	public static final int STATE_NON_INDEXABLE = 3;
	public static final int STATE_BAD_ORIENTATION = 4;
	public static final int STATE_MOVING = 5;
	public static final int SNAPSHOT_ID_INVALID = -1;

	public static class PoiVote {
		public Poi poi;
		public float fVote;
	}

	/**
	 * public interface
	 */
	public interface Listener {
		void onStateChanged(int iStateId, Poi poi, long lSnapshotId);
		void onSnapshot(Snapshot snapshot);
	}

	public void start() {

		if (!googleApiClient.isConnected()) {
			googleApiClient.connect();
		}
		Pexeso.restartLiveFeed();	// TODO: [SY] should be called by camera
	}

	public void stop() {
		locationApi.removeLocationUpdates(googleApiClient, this);
		Pexeso.stopLiveFeed();
	}

	public Urbo setListener(Listener listener) {
		Pexeso.setListener(listener);
		return this;
	}

	public Urbo setDebugListener(DebugListener listener) {
		debugListener = listener;
		return this;
	}

	/**
	 * @return List of POIs (points of interest) that are currently in cache
	 */
	public Poi[] getPoiShortlist() {
		return Pexeso.getPoiShortlist();
	}

	public void tagSnapshot(@NonNull Snapshot snapshot, @NonNull Poi poi) {
		Pexeso.tagSnapshot(snapshot, poi);
	}

	public void confirmRecognition(long lSnapshotId) {
		Pexeso.confirmRecognition(lSnapshotId);
	}

	public void rejectRecognition(long lSnapshotId) {
		Pexeso.rejectRecognition(lSnapshotId);
	}

	public boolean getSnapshot(long lSnapshotId) {
		return Pexeso.getSnapshot(lSnapshotId);
	}

	public boolean takeSnapshot() {
		return Pexeso.takeSnapshot();
	}

	@SuppressWarnings("unused")
	public Poi identify(@NonNull byte[] baImg) {
		throw new UnsupportedOperationException();
	}

	public void forceCacheRefresh() {
		Pexeso.forceCacheRefresh();
	}

// Events

	@Override
	public void onConnected(Bundle dataBundle) {
		Log.d(TAG, "Connected to Google location services");

		// push the last known location
		Pexeso.pushLocation(locationApi.getLastLocation(googleApiClient));

		// start periodic updates according to the locationRequest
		locationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Log.w(TAG, "Disconnected from Google location services");
		googleApiClient.connect();
	}

	@Override
	public void onLocationChanged(Location location) {
		Pexeso.pushLocation(location);
	}

	// TODO: take care of batch requests and retries
	void onRecognition(final Snapshot snapshot) {
		xsIo.execute(new Runnable() {
			@Override
			public void run() {
				odieBlob.uploadImage(snapshot.getImgFile());
				Odie.PutResponse putResponse = null;
				try {
					Odie.PutRequest putRequest = new Odie.PutRequest();
					if (snapshot.getPoi().isClientOnly()) {
						putRequest.pois = new Poi[]{snapshot.getPoi()};
					}
					else {
						putRequest.pois = new Poi[0];
					}
					putRequest.recognitionEvents = new RecoEvent[]{snapshot};

					putResponse = odie.sync(putRequest);
				}
				catch (Throwable e) {
					Log.e("Urbo", "PUT /pois failed " + e);
				}

				if (putResponse == null) {
					Log.e(TAG, "putResponse == null");
					// TODO: keep locally to retry later
					return;
				}
				Pexeso.poiCacheUpdateCallback(putResponse);
			}
		});
	}

	void onCacheRequest(final int iRequestId, final Location location) {
		xsIo.execute(new Runnable() {
			long hitMeAgainIn = 0;

			@Override
			public void run() {
				if (hitMeAgainIn > 0) {
					SystemClock.sleep(hitMeAgainIn * 1000);
				}
				Odie.OdieUpdate odieResponse = null;
				try {
					odieResponse = odie.getPois(location.getLatitude(),
							location.getLongitude(), location.getAccuracy(),
							sCountryCode, sDeviceId, false);
				}
				catch (Throwable e) {
					Log.e("Urbo", "GET /pois failed " + e);
				}

				if (odieResponse == null) {
					Log.e(TAG, "odieResponse[" + iRequestId + "] == null");
					Pexeso.poiCacheRequestCallback(iRequestId, location, null);
					return;
				}

				if (odieResponse.hitMeAgainIn > 0) {
					Log.i(TAG, "odieResponse[" + iRequestId + "] gives " +
							odieResponse.pois.length + ", hitMeAgainIn " + hitMeAgainIn);
					hitMeAgainIn = odieResponse.hitMeAgainIn;
					xsIo.execute(this);
					return;
				}

				Log.i(TAG, "odieResponse[" + iRequestId + "] gives " + odieResponse.pois.length);
				if (Pexeso.poiCacheRequestCallback(iRequestId, location, odieResponse.pois)) {
					odieBlob.setBucket(odieResponse.s3Bucket, odieResponse.s3Folder);
				}
			}
		});
	}

	void onError(String sTag, String sMsg, Throwable e) {
		Log.e(sTag, sMsg, e);
	}

	void onError(int severity, String sMsg) {
	    if (severity == Log.INFO && debugListener != null) {
		    String[] strings = sMsg.split("\t");
		    if (strings.length == 2) {
			    debugListener.setField(strings[0].trim(), strings[1].trim());
		    }
	    }
	    if (severity == Log.ERROR && debugListener != null) {
		    debugListener.toast(sMsg);
	    }
		Log.println(severity, TAG, sMsg);
	}

	// TODO: the method below should be implemented in C++
	void onSnapshotImageReady(String imgFileName, YuvImage yuvImage) {
		File imageFile = new File(params.fImgDir, imgFileName);
		try {
			FileOutputStream fos = new FileOutputStream(imageFile);
			Rect rcImage = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());
			yuvImage.compressToJpeg(rcImage, 90, fos);
			fos.close();
			Log.w(TAG, "new file " + imageFile.getPath() + " " + imageFile.length());
		}
		catch (Exception e) {
			Log.e(TAG, "failed to write jpegBuffer", e);
		}
	}

	public boolean connectLocationService() {
		boolean gps_enabled = false;
		try {
			gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		}
		catch (Exception ex){}

		return gps_enabled;
	}

	public Location getCurrentLocation() {
		return Pexeso.getCurrentLocation();
	}

	private static String getDeviceId() {
		String key;
		try {
			key = new BigInteger(1, MessageDigest.getInstance("MD5")
					.digest(Build.SERIAL.getBytes())).toString(16);
		}
		catch (NoSuchAlgorithmException e) {
			Log.w("RecoEvent", "Could not hash the key", e);
			key = Build.SERIAL + Math.random();
		}
		return key.substring(0, 8);
	}

}

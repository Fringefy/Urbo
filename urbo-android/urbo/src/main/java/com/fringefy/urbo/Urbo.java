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
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Urbo implements LocationListener, GoogleApiClient.ConnectionCallbacks {
	private static final String TAG = "Urbo";

// Inner Types

	static class Params {
		// location update frequency (milliseconds), see location services docs
		private static final int UPDATE_INTERVAL = 5000;

		// fastest update frequency (milliseconds), see location services docs
		private static final int FASTEST_INTERVAL = 1000;

		private String sEndpoint = "https://qaodie.fringefy.com/odie";
		private static final int MAX_ODIE_CONNECTIONS = 2;

		File fImgDir;
	}

	public interface OdieUpdateObserver {
		void onOdieResponse(OdieUpdate odieUpdate);
	}


// Private Fields

	static private Urbo urbo = null;
	Params params;

	private HashSet<OdieUpdateObserver> odieUpdateObservers = new HashSet<>();

    // TODO: [AC] initialize these in the constructor
    private String sDeviceId, sCountryCode;

	private Odie odie;
	private OdieBlob odieBlob;
	private ExecutorService xsIo; // TODO: shift executor to C++

	private LocationManager locationManager;
	private GoogleApiClient googleApiClient;
	private FusedLocationProviderApi locationApi;
	private LocationRequest locationRequest;
	private DebugListener debugListener;


// Construction

    // bind hardware
	private Urbo(@NonNull Context context) {
		Pexeso.init(null, this);

		params = new Params();
		params.fImgDir = context.getCacheDir();

		sDeviceId = Build.SERIAL;

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

		// setup ODIE
		odie = OdieFactory.getInstance(params.sEndpoint, "RnNmIgxtZzcajIZww7NlKnAeYwTjOq9xp9Xu7YkS");
		odieBlob = new OdieBlob();
		xsIo = Executors.newFixedThreadPool(Params.MAX_ODIE_CONNECTIONS);
	}

	/**
	 * This method will create and load Urbo. <br>
	 * @param context The context of application that will use Urbo.
	 * @return Return initialized and ready Urbo.
	 */
	public static Urbo getInstance(@NonNull Context context) {
		if (urbo != null) {
			return urbo;
		}
		return (urbo = new Urbo(context));
	}


// Public Interface

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
	}

	public void stop() {
		locationApi.removeLocationUpdates(googleApiClient, this);
	}

	public Urbo setListener(Listener listener) {
		Pexeso.setListener(listener);
		return this;
	}

	public Urbo setDebugListener(DebugListener listener) {
		debugListener = listener;
		return this;
	}

	public Urbo addOdieUpdateObserver(OdieUpdateObserver odieUpdateObserver) {
		odieUpdateObservers.add(odieUpdateObserver);
		return this;
	}

	/**
	 * @return List of POIs (points of interest) that are currently in cache
	 */
	public Poi[] getPoiShortlist(boolean bSort) {
		return Pexeso.getPoiShortlist(bSort);
	}

	public void tagSnapshot(@NonNull Snapshot snapshot, @NonNull Poi poi) {
		Pexeso.tagSnapshot(snapshot, poi);
	}

	public void confirmRecognition(long lSnapshotId) {
		Pexeso.confirmRecognition(lSnapshotId);
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

	/**
	 * @return Gson that can be used to backup and restore local RecoEvent history
	 */
	public Gson getGson() {
		return OdieFactory.getGson();
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
				odieBlob.uploadImage(new File(params.fImgDir, snapshot.getImgFileName()));
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
				for (Map.Entry<String, String> entry : putResponse.pois.syncList.entrySet()) {
					if (snapshot.getPoi().getId().equals(entry.getKey())) {
						snapshot.getPoi().setId(entry.getValue());
					}
				}
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
				OdieUpdate odieResponse = null;
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
					for (OdieUpdateObserver observer : odieUpdateObservers) {
						observer.onOdieResponse(odieResponse);
					}
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
					if (odieResponse.tabpageUrl != null) {
						Poi.tabpageUrl = odieResponse.tabpageUrl;
					}
					for (OdieUpdateObserver observer : odieUpdateObservers) {
						observer.onOdieResponse(odieResponse);
					}
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
		// TODO: if size of accumulated JPG files in cache exceeds the threshold, delete older ones
		try {
			FileOutputStream fos = new FileOutputStream(imageFile);
			Rect rcImage = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());
			yuvImage.compressToJpeg(rcImage, 90, fos);
			fos.close();
		}
		catch (Exception e) {
			Log.e(TAG, "failed to write jpegBuffer", e);
		}
	}

	public boolean connectLocationService() {
		boolean gps_enabled = false;
		try{
			gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		}
		catch(Exception ex){}

		return gps_enabled;
	}

	public Location getCurrentLocation() {
		return Pexeso.getCurrentLocation();
	}

}

package com.fringefy.urbo;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Urbo implements LocationListener {
	private static final String TAG = "Urbo";

// Inner Types

	public static class Params {
		// location update frequency (milliseconds), see location services docs
		private static final int UPDATE_INTERVAL = 5000;
		// ignore location updates that are 5 times less accurate than current location
		private static final float IGNORE_INACCURATE_UPDATES = 5;

		public String sEndpoint = "https://odie.fringefy.com/odie";
		public String sApiKey;
		private static final int MAX_ODIE_CONNECTIONS = 2;

		boolean bIdentify = true;

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
	private DebugListener debugListener;

	private float bestAccuracy = Float.MAX_VALUE;

// Construction

	private Urbo(@NonNull Context context, @NonNull Params initParams) {

		params = initParams;
		sDeviceId = getDeviceId();
		xsIo = Executors.newFixedThreadPool(Params.MAX_ODIE_CONNECTIONS);

		odieBlob = new OdieBlob();

		odie = OdieFactory.getInstance(params.sEndpoint, params.sApiKey);
		urbo = this;
		if (context instanceof DebugListener) {
			debugListener = (DebugListener) context;
		}

		Pexeso.init(null, this);

		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Pexeso.pushLocation(lastKnownLocation());
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

	public static class PoiVote {
		public Poi poi;
		public float fVote;
	}

	/**
	 * public interface
	 */
	public interface Listener {
		void onStateChanged(int iStateId, Snapshot snapshot);
		void onSnapshot(Snapshot snapshot);
	}

	public void start() {

		for (String provider: locationManager.getAllProviders()) {
			locationManager.requestLocationUpdates(provider, Params.UPDATE_INTERVAL, 0, this);
		}
		Pexeso.restartLiveFeed();	// TODO: [SY] should be called by camera
	}

	public void stop() {
		try {
			locationManager.removeUpdates(this);
		}
		catch (RuntimeException e) {
			Log.e(TAG, "stop locationApi failed", e);
		}

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

	private Location lastKnownLocation() {
		long bestTime = Long.MAX_VALUE;
		long minTime = Long.MAX_VALUE;
		Location bestResult = null;

		for (String provider: locationManager.getAllProviders()) {
			Location location = locationManager.getLastKnownLocation(provider);
			if (location != null) {
				float accuracy = location.getAccuracy();
				long time = location.getTime();

				if (time < minTime && accuracy < bestAccuracy) {
					bestResult = location;
					bestAccuracy = accuracy;
					bestTime = time;
				}
				else if (time < minTime &&
						bestAccuracy == Float.MAX_VALUE && time > bestTime) {
					bestResult = location;
					bestTime = time;
				}
			}
		}
		return bestResult;
	}
	/**
	 * @return List of POIs (points of interest) that are currently in cache
	 */
	public Poi[] getPoiShortlist() {
		Pexeso.pushLocation(lastKnownLocation());
		return Pexeso.getPoiShortlist();
	}

	public void tagSnapshot(@NonNull Snapshot snapshot, @NonNull Poi poi) {
		Pexeso.tagSnapshot(snapshot, poi);
	}

	public void confirmRecognition(@NonNull Snapshot snapshot) {
		Pexeso.confirmRecognition(snapshot);
	}

	public void rejectRecognition(@Nullable Snapshot snapshot) {
		Pexeso.rejectRecognition(snapshot);
	}

	public boolean takeSnapshot() {
		return Pexeso.takeSnapshot();
	}

	@SuppressWarnings("unused")
	public Poi identify(@NonNull byte[] baImg) {
		throw new UnsupportedOperationException();
	}

	public void forceCacheRefresh() {
		Pexeso.pushLocation(lastKnownLocation());
		Pexeso.forceCacheRefresh();
	}

// Events

	@Override
	public void onLocationChanged(Location location) {

		if (location.getAccuracy() < bestAccuracy) {
			bestAccuracy = location.getAccuracy();
		}
		if (location.getAccuracy() > bestAccuracy * params.IGNORE_INACCURATE_UPDATES) {
			return; // ignore non-accurate location updates
		}
		Pexeso.pushLocation(location);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	// TODO: take care of batch requests and retries
	void sendRecoEvent(final Snapshot snapshot) {
		xsIo.execute(new Runnable() {
			@Override
			public void run() {
				odieBlob.uploadImage(snapshot.imgFileName, snapshot.jpegBuffer);
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

	void sendCacheRequest(final int iRequestId, final Location location) {
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
							sCountryCode, sDeviceId);
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
		Log.println(severity, TAG, sMsg);
		if (severity == Log.INFO && debugListener != null) {
			String[] strings = sMsg.split("\t");
			if (strings.length == 2) {
				debugListener.setField(strings[0].trim(), strings[1].trim());
				return;
			}
		}
		if (severity == Log.ERROR && debugListener != null) {
			debugListener.toast(sMsg);
		}
	}

	public boolean connectLocationService() {
		boolean gps_enabled = false;
		try {
			gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			Pexeso.pushLocation(lastKnownLocation());
		}
		catch (Exception ex){}

		return gps_enabled;
	}

	public Location getCurrentLocation() {
		Pexeso.pushLocation(lastKnownLocation());
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

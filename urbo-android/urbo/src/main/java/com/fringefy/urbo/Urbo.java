package com.fringefy.urbo;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO: [AC] Make the singleton aware of activity renewal etc.
public class Urbo implements LocationListener, Pexeso.Listener, ConnectionCallbacks {

// Inner Types

	private static class Params {
		// location update frequency (milliseconds), see location services docs
		private static final int UPDATE_INTERVAL = 5000;

		// fastest update frequency (milliseconds), see location services docs
		private static final int FASTEST_INTERVAL = 1000;

		private static final String sEndpoint = "http://qaodie.fringefy.com/odie";

		private static File fImgDir;
	}

	public interface PoiCacheObserver {
		public void onListChanged(List<Poi> lstPois);
	}


// Private Fields

	static Urbo urbo = null;

	private Activity activity;
	private PoiCacheObserver poiCacheObserver;

    // TODO: [AC] initialize these in the constructor
    private String sDeviceId, sCountryCode;

	private volatile List<Poi> poiCache;

	private Odie odie;
	private OdieBlob odieBlob;
	private ExecutorService xsIo;

	private GoogleApiClient googleApiClient;
	private FusedLocationProviderApi locationApi;
	private LocationRequest locationRequest;


// Construction

    // bind hardware
	private Urbo(@NonNull Activity activity) {
		this.activity = activity;
		Pexeso.init(null, this);

		// create the LocationRequest object
		locationRequest = LocationRequest.create();
		// use high accuracy
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// set the update intervals
		locationRequest.setInterval(Params.UPDATE_INTERVAL);
		locationRequest.setFastestInterval(Params.FASTEST_INTERVAL);

		locationApi = LocationServices.FusedLocationApi;

		googleApiClient = new GoogleApiClient.Builder(activity)
				.addApi(LocationServices.API)
				.addConnectionCallbacks(this)
				.build();

		// connect to the Google API
		googleApiClient.connect();

		// setup ODIE
		odie = OdieFactory.getInstance(Params.sEndpoint);
		odieBlob = new OdieBlob(null, null, Params.fImgDir = activity.getCacheDir());
		xsIo = Executors.newSingleThreadExecutor();
	}

	public static Urbo load(@NonNull Activity activity) {
		if (urbo != null) {
			throw new IllegalStateException("Urbo can not be instantiated more than once");
		}

		return (urbo = new Urbo(activity));
	}


// Public Interface

	public void start() {

		if (!googleApiClient.isConnected()) {
			googleApiClient.connect();
		}
	}

	public void stop() {
		locationApi.removeLocationUpdates(googleApiClient, this);
	}

	@SuppressWarnings("unused")
	public void setPoiListObserver(PoiCacheObserver poiCacheObserver) {
		this.poiCacheObserver = poiCacheObserver;
	}

	@SuppressWarnings("unused")
	public List<Poi> getPoiCache() {
		return poiCache;
	}

	@SuppressWarnings("unused")
	public void tagPoi(@NonNull Poi poi, @NonNull Snapshot snapshot) {

	}

	@SuppressWarnings("unused")
	public void tagPoi(@NonNull Poi poi, @NonNull byte[] baImg) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unused")
	public Poi identify(@NonNull byte[] baImg) {
		throw new UnsupportedOperationException();
	}


// Events

	@Override
	public void onConnected(Bundle dataBundle) {
		Log.d("Urbo", "Connected to Google location services");

		// push the last known location
		Pexeso.pushLocation(locationApi.getLastLocation(googleApiClient));

		// start periodic updates according to the locationRequest
		locationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Log.w("Urbo", "Disconnected from Google location services");
		googleApiClient.connect();
	};

	@Override
	public void onLocationChanged(Location location) {
		Pexeso.pushLocation(location);
	}

    void onError(String sTag, String sMsg, Throwable e) {
        Log.e(sTag, sMsg, e);
    }

	@Override
	public void onCacheRequest(final int iRequestId, final Location location) {
		xsIo.execute(new Runnable() {
			@Override
			public void run() {
				Odie.Pois poisResponse = odie.getPois(location.getLatitude(),
                    location.getLongitude(), location.getAccuracy(),
                    sCountryCode, sDeviceId, false);

                Pexeso.poiCacheRequestCallback(iRequestId, poisResponse.pois);
			}
		});
	}

    @Override
    public void onError(int severity, String sMsg) {
        Log.println(severity, "Urbo", sMsg);
    }
}

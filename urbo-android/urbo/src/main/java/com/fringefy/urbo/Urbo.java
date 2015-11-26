package com.fringefy.urbo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.YuvImage;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

		private String sEndpoint = "https://stodie.fringefy.com/odie";
		private static final int MAX_ODIE_CONNECTIONS = 2;

		File fImgDir;
	}

	public interface PoiCacheObserver {
		void onListChanged(List<Poi> lstPois);
	}


// Private Fields

	static private Urbo urbo = null;
	Params params;
	RenderScript rs; // used in convertYuvImageToBitmap()

	private PoiCacheObserver poiCacheObserver;

    // TODO: [AC] initialize these in the constructor
    private String sDeviceId, sCountryCode;

	private volatile List<Poi> poiCache = new ArrayList<>(); // TODO: remove when the request to native is built

	private Odie odie;
	private OdieBlob odieBlob;
	private ExecutorService xsIo; // TODO: shift executor to C++

	private GoogleApiClient googleApiClient;
	private FusedLocationProviderApi locationApi;
	private LocationRequest locationRequest;
	private Listener listener;
	private DebugListener debugListener;
	private ImageView imageViewForSnapshot;

// Construction

    // bind hardware
	private Urbo(@NonNull Context context) {
		Pexeso.init(null, this);

		params = new Params();
		params.fImgDir = context.getCacheDir();
		rs = RenderScript.create(context);

		sDeviceId = Build.SERIAL;

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
		odie = OdieFactory.getInstance(params.sEndpoint);
		odieBlob = new OdieBlob(params.fImgDir);
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

	public static class PoiVote {
		PoiVote(Poi poi, float fVote) {
			this.poi = poi;
			this.fVote = fVote;
		}
		Poi poi;
		float fVote;
	}

	/**
	 * public interface
	 */
	public interface Listener {
		// TODO: maybe we prefer to have onStateChanged(void) and call onSnapshot() for STATE_RECOGNIZED?
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

	@SuppressWarnings("unused")
	public Urbo setPoiListObserver(PoiCacheObserver poiCacheObserver) {
		this.poiCacheObserver = poiCacheObserver;
		return this;
	}

	/**
	 *
	 * @return List of Poi's (point of interest) that currently in cache
	 */
	public List<Poi> getPoiCache() {
		return poiCache;
	}

	public void tagSnapshot(@NonNull Snapshot snapshot, @NonNull Poi poi) {
		//Pexeso.tagSnapshot(snapshot, poi);
	}

	public boolean getSnapshot(long lSnapshotId) {
		return Pexeso.getSnapshot(lSnapshotId);
	}

	public boolean takeSnapshot() {
		return Pexeso.takeSnapshot();
	}

	public Urbo setDisplayView(ImageView imageView) {
		imageViewForSnapshot = imageView;
		return this;
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

    void onError(String sTag, String sMsg, Throwable e) {
	    Log.e(sTag, sMsg, e);
    }

	// TODO: take care of batch requests and retries
	void onNewRecognition(final Snapshot snapshot) {
		xsIo.execute(new Runnable() {
			@Override
			public void run() {
				Odie.PutResponse putResponse = null;
				try {
					Odie.PutRequest putRequest = new Odie.PutRequest();
					if (snapshot.getPoi().isClientOnly()) {
						putRequest.pois = new Poi[]{snapshot.getPoi()};
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
					if (snapshot.getPoi().getId() == entry.getKey()) {
						snapshot.getPoi().setId(entry.getValue());
						poiCache.add(snapshot.getPoi());
					}
				}
			}
		});
	}

	void onCacheRequest(final int iRequestId, final Location location) {
		xsIo.execute(new Runnable() {
			@Override
			public void run() {
				Odie.Pois poisResponse = null;
				try {
					poisResponse = odie.getPois(location.getLatitude(),
							location.getLongitude(), location.getAccuracy(),
							sCountryCode, sDeviceId, false);
				}
				catch (Throwable e) {
					Log.e("Urbo", "GET /pois failed " + e);
				}

				if (poisResponse == null) {
					Log.e(TAG, "poisResponse[" + iRequestId + "] == null");
					Pexeso.poiCacheRequestCallback(iRequestId, location, null);
					return;
				}

				for (Poi poi : poisResponse.pois) {
					poi.handleLegacyServer();
				}

				Log.i(TAG, "poisResponse[" + iRequestId + "] gives " + poisResponse.pois.size());
				if (Pexeso.poiCacheRequestCallback(iRequestId, location, poisResponse.pois)) {
					// TODO: accept the fields like poisResponse.sharepageUrl
					poiCache = poisResponse.pois;
					odieBlob.setBucket(poisResponse.s3Bucket, poisResponse.s3Folder);
				}
			}
		});
	}

	void onError(int severity, String sMsg) {
	    if (severity == Log.INFO && debugListener != null) {
		    String[] strings = sMsg.split("\t");
		    if (strings.length == 2) {
			    debugListener.setField(strings[0], strings[1]);
		    }
	    }
	    if (severity == Log.ERROR && debugListener != null) {
		    debugListener.toast(sMsg);
	    }
	    Log.println(severity, TAG, sMsg);
	}

	/**
	 * Upload an image file to the Odie server.
	 * @param fImg The image file to upload
	 */
	void uploadImage(File fImg) {
		try {
			odieBlob.uploadImage(fImg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void onSnapshotImageReady(final File fImg) {
		if (imageViewForSnapshot != null) {
			imageViewForSnapshot.post(new Runnable() {
				@Override
				public void run() {
					imageViewForSnapshot.setImageURI(Uri.fromFile(fImg));
				}
			});
		}
	}

	void onSnapshotImageReady(final Bitmap bmp) {
		if (imageViewForSnapshot != null) {
			imageViewForSnapshot.post(new Runnable() {
				@Override
				public void run() {
					imageViewForSnapshot.setImageBitmap(bmp);
					bmp.recycle();
				}
			});
		}
	}

	void onSnapshotImageReady(final YuvImage yuvImage) {
		if (imageViewForSnapshot != null) {
			imageViewForSnapshot.post(new Runnable() {
				@Override
				public void run() {
					Bitmap bmp = convertYuvImageToBitmap(yuvImage);
					imageViewForSnapshot.setImageBitmap(bmp);
					bmp.recycle();
				}
			});
		}
	}

	private ScriptIntrinsicYuvToRGB yuvToRgb;
	private Type.Builder yuvType;
	private Type.Builder rgbaType;
	private Allocation yuvAllocation;
	private Allocation rgbaAllocation;

	// fast enough to run on UI thread
	private synchronized Bitmap convertYuvImageToBitmap(YuvImage yuvImage) {

		int w = yuvImage.getWidth();
		int h = yuvImage.getHeight();
		RenderScript rs = Urbo.urbo.rs;

		if (yuvToRgb == null) { // once
			yuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
		}

		if (yuvAllocation == null ||
				yuvAllocation.getBytesSize() < yuvImage.getYuvData().length) {
			yuvType = new Type.Builder(rs,
					Element.U8(rs)).setX(yuvImage.getYuvData().length);
			yuvAllocation = Allocation.createTyped(rs,
					yuvType.create(), Allocation.USAGE_SCRIPT);
		}

		if (rgbaAllocation == null ||
				rgbaAllocation.getBytesSize() <
						rgbaAllocation.getElement().getBytesSize() * w * h) {
			rgbaType = new Type.Builder(rs,
					Element.RGBA_8888(rs)).setX(w).setY(h);
			rgbaAllocation = Allocation.createTyped(rs,
					rgbaType.create(), Allocation.USAGE_SCRIPT);
		}

		yuvAllocation.copyFrom(yuvImage.getYuvData());

		yuvToRgb.setInput(yuvAllocation);
		yuvToRgb.forEach(rgbaAllocation);

		Bitmap bmpout = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		rgbaAllocation.copyTo(bmpout);
		return bmpout;
	}
}

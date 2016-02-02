package com.fringefy.urbo;

import android.location.Location;

import java.io.File;

abstract class Pexeso {

// Construction

	static {
		System.loadLibrary("pexeso_android");
	}

	static void init(File paramsFile, Urbo urbo) {

		String sParamsFile = null;
		if (paramsFile != null && paramsFile.exists() && paramsFile.canRead()) {
			sParamsFile = paramsFile.getAbsolutePath();
		}

		init(sParamsFile, urbo);
	}


// JNI Imports

	private static native void init(String sParamsFile, Urbo urbo);
	static native void initLiveFeed(int w, int h, int rotation,
		@SuppressWarnings("deprecation") android.hardware.Camera camera);
	static native void setListener(Urbo.Listener urboListener);
	static native void stopLiveFeed();
	static native void restartLiveFeed();
	static native void pushFrame(byte[] baImg);
	static native void pushHeading(float fHeading);
	static native void pushPitch(float fPitch);
	static native void pushLocation(Location location);
	static native void forceCacheRefresh();
    static native boolean takeSnapshot();
	static native void confirmRecognition(Snapshot snapshot);
	static native void rejectRecognition(Snapshot snapshot);
	static native void tagSnapshot(Snapshot snapshot, Poi poi);
	static native Poi[] getPoiShortlist();
	static native Location getCurrentLocation();

	// TODO: these callbacks will be gone, simply returned by Java when Urbo.xsIo will be migrated to C++
	static native boolean poiCacheRequestCallback(int iRequestId, Location location, Poi[] pois);
	static native void poiCacheUpdateCallback(Odie.PutResponse putResponse);
}

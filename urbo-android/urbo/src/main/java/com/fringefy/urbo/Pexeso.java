package com.fringefy.urbo;

import android.location.Location;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

abstract class Pexeso {

// Inner Classes

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
	static native void initLiveFeed(int w, int h, @SuppressWarnings("deprecation") android.hardware.Camera camera);
	static native void setListener(Urbo.Listener urboListener);
	static native void stopLiveFeed();
	static native void pushFrame(byte[] baImg);
	static native void pushHeading(float fHeading);
	static native void pushPitch(float fPitch);
	static native void pushLocation(Location location);
	static native void forceCacheRefresh();
	static native boolean poiCacheRequestCallback(int iRequestId, Location location, Poi[] pois);
    static native boolean takeSnapshot();
	static native boolean confirmRecognition(long lSnapshotId);
	static native boolean tagSnapshot(Snapshot snapshot, Poi poi);
	static native boolean getSnapshot(long lSnapshotId);
	static native Poi[] getPoiShortlist(boolean bSort);
	static native Location getCurrentLocation();
}

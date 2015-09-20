package com.fringefy.urbo;

import android.location.Location;
import android.os.Handler;

import java.io.File;
import java.util.List;

import retrofit.android.AndroidLog;

// TODO: [AC] Make Pexeso package private again,
//  the states should somehow be public (maybe different class?)
public abstract class Pexeso {

    public static final int STATE_SEARCH = 0;
	public static final int STATE_RECOGNITION = 1;
	public static final int STATE_NO_RECOGNITION = 2;
	public static final int STATE_NON_INDEXABLE = 3;
	public static final int STATE_BAD_ORIENTATION = 4;


// Inner Classes

	interface Listener {
		void onCacheRequest(int iRequestId, Location location);
		void onError(int severity, String sMsg);
	}

    interface LiveFeedListener {
        void onNewBuffer(byte[] baBuf);
        boolean onStateChanged(int iStateId, Poi poi, long lSnapshotId);
        void onSnapshot(RecoEvent recoEvent);
    }


// Fields




// Construction

	static {
		System.loadLibrary("pexeso_android");
	}

    static void init(File paramsFile, Listener pexesoListener) {

		String sParamsFile = null;
		if (paramsFile != null && paramsFile.exists() && paramsFile.canRead()) {
			sParamsFile = paramsFile.getAbsolutePath();
		}

		init(sParamsFile, pexesoListener);
	}


// JNI Imports

	private static native void init(String sParamsFile, Listener pexesoListener);
	static native void initLiveFeed(int w, int h, LiveFeedListener feedListener);
	static native synchronized byte[] pushFrame(byte[] baImg);
	static native void pushHeading(float fHeading);
	static native void pushPitch(float fPitch);
	static native void pushLocation(Location location);
	static native void poiCacheRequestCallback(int iRequestId, List<Poi> pois);
    static native void takeSnapshot();
	static native RecoEvent getSnapshot(long lSnapshotId);
}

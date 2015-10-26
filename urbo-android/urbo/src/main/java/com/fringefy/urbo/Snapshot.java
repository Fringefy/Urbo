package com.fringefy.urbo;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class Snapshot extends RecoEvent {

	private static final String TAG = "Snapshot";

	private Poi poi;

	private Urbo.PoiVote[] votes = new Urbo.PoiVote[0];

	Snapshot(@NonNull Location location, float fPitch, float fAzimuth,
	         @Nullable Poi machineSelectedPoi, @Nullable String sClientUna) {
		super(location, fPitch, fAzimuth, machineSelectedPoi, sClientUna);
		this.poi = machineSelectedPoi;
	}

	Poi getPoi() {
		return poi;
	}

	public Urbo.PoiVote[] getVotes() {
		return votes;
	}

	// TODO: methods below should be implemented in C++

	void setPoi(@NonNull Poi poi) {
		super.setUserSelectedPoi(poi);
		this.poi = poi;
	}

	private int nVotes;
	void setVotes(int size) {
		votes = new Urbo.PoiVote[size];
		nVotes = 0;
	}

	void addVote(Poi poi, float fVote) {
		votes[nVotes++] = new Urbo.PoiVote(poi, fVote);
	}

	void onImageReady(YuvImage yuvImage, Urbo urbo) {
		if (yuvImage == null) {
			return;
		}
		urbo.onSnapshotImageReady(yuvImage);
		try {
			File imageFile = new File(urbo.params.fImgDir, getImgFileName());
			FileOutputStream fos = new FileOutputStream(imageFile);
			Rect rcImage = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());
			yuvImage.compressToJpeg(rcImage, 90, fos);
			fos.close();
		}
		catch (Exception e) {
			Log.e(TAG, "failed to write jpegBuffer", e);
		}
	}
}
package com.fringefy.urbo;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;

public class Snapshot extends RecoEvent {

	transient private Poi poi;
	transient private Urbo.PoiVote[] votes = new Urbo.PoiVote[0];
	transient File imageFile;

	Snapshot(@NonNull Location location, float fPitch, float fAzimuth, float fVelocity,
	         @Nullable Poi machineSelectedPoi, @Nullable String sClientUna) {
		super(location, fPitch, fAzimuth, fVelocity, machineSelectedPoi, sClientUna);
		this.poi = machineSelectedPoi;
		this.imageFile = new File(Urbo.getInstance().params.fImgDir, imgFileName);
	}

	Poi getPoi() {
		return poi;
	}

	public Urbo.PoiVote[] getVotes() {
		return votes;
	}

	public File getImgFile() {
		return imageFile;
	}

	@Override
	protected void finalize() throws Throwable {
		if (imageFile != null && imageFile.exists()) {
			Log.w("Urbo", "delete file " + imageFile.getName() + " " + imageFile.length() + " on finalize");
			imageFile.delete();
		}
		super.finalize();
	}
}
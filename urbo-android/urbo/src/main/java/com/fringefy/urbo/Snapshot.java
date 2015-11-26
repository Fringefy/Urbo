package com.fringefy.urbo;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

	void setPoi(@NonNull Poi poi) {
		setUserSelectedPoi(poi);
		this.poi = poi;
	}
}
package com.fringefy.urbo;

import java.util.Date;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Represents a recognition event. This class can be serialized with GSON to comply
 * with the ODIE back-end. This is a POJO class with not interesting logic.
 */
class RecoEvent {

	/** the client's timestamp */
	private Date clientTimestamp;
	private String machineSelectedPoi;

	private String userSelectedPoi = null;
	/** client location, accuracy, and camera azimuth */
	private float[] loc;
	private float locAccuracy;
	float pitch, camAzimuth, velocity;
	String imgFileName;

	private String clientGeneratedUNA;
	private boolean isIndex = false;
	private boolean userFeedback = false;

	private String deviceID;

	public String getMachineSelectedPoiId() {
		return machineSelectedPoi;
	}

	public Long getTime() {
		return clientTimestamp.getTime();
	}

	public Location getLocation() {
		Location location = new Location(imgFileName);
		location.setAccuracy((float)locAccuracy);
		location.setLatitude(loc[Constants.LAT]);
		location.setLongitude(loc[Constants.LONG]);
		location.setTime(clientTimestamp.getTime());
		// TODO: consider setting bearing, speed, altitude
		return location;
	}
}

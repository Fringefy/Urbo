package com.fringefy.urbo;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Represents a recognition event. This class can be serialized with GSON to comply
 * with the ODIE back-end. This is a POJO class with not interesting logic.
 */
class RecoEvent {

	/** the client's timestamp */
	private final Date clientTimestamp;
	private final String machineSelectedPoi;
	public String getMachineSelectedPoiId() {
		return machineSelectedPoi;
	}
	public boolean isTagged() {
		return userSelectedPoi != null;
	}

	private String userSelectedPoi = null;
	/** client location, accuracy, and camera azimuth */
	private final double[] loc = new double[] { Double.NaN, Double.NaN};
	private double locAccuracy;
	public Location getLocation() {
		Location location = new Location(imgFileName);
		location.setAccuracy((float)locAccuracy);
		location.setLatitude(loc[Constants.LAT]);
		location.setLongitude(loc[Constants.LONG]);
		location.setTime(clientTimestamp.getTime());
		// TODO: consider setting bearing, speed, altitude
		return location;
	}

	float pitch, camAzimuth, velocity;
	final String imgFileName;

	private String clientGeneratedUNA;
	private boolean isIndex = false;
	private boolean userFeedback = false;

	private final String deviceID;

	protected RecoEvent(@NonNull Location location,
			float fPitch, float fAzimuth, float fVelocity,
			@Nullable Poi machineSelectedPoi, @Nullable String sClientUna) {

		deviceID = Urbo.getInstance().sDeviceId;
		clientTimestamp = new Date(); // TODO: should be based on frame arrival
		imgFileName = deviceID + "." + clientTimestamp.getTime() + ".jpg";

		loc[Constants.LAT] = location.getLatitude();
		loc[Constants.LONG] = location.getLongitude();
		locAccuracy = location.getAccuracy();

		pitch = fPitch;
		camAzimuth = fAzimuth;
		velocity = fVelocity;

		if (machineSelectedPoi == null) {
			this.machineSelectedPoi = null;
		}
		else {
			this.machineSelectedPoi = machineSelectedPoi.getId();
		}
		clientGeneratedUNA = sClientUna;
	}
}

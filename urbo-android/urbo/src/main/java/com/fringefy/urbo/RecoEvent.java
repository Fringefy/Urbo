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
	protected final Date clientTimestamp;

	protected String machineSelectedPoi = null;
	protected String userSelectedPoi = null;

	/** client location, accuracy, and camera azimuth */
	protected final double[] loc = new double[2];
	protected double locAccuracy;
	public double getCamAzimuth() { return azimuth; }
	float pitch, azimuth;

	private final String imgFileName;
	public String getImgFileName() { return imgFileName; }

	private String clientGeneratedUNA;

	String getUnaForLocalPoi() {
		return clientGeneratedUNA;
	}

	/**
	 * during a recognition event, If the UNA array in the userSelectedPoi is empty,
	 * add isIndex=true to the current recognition event
	 */
	protected boolean isIndex = false;
	protected boolean userFeedback = true;

	protected final String deviceID;

	protected RecoEvent(@NonNull Location location, float fPitch, float fAzimuth,
			  @Nullable Poi machineSelectedPoi, @Nullable String sClientUna) {

		deviceID = Build.SERIAL;
		clientTimestamp = new Date();

		String sKey;
		try {
			sKey = new BigInteger(1, MessageDigest.getInstance("MD5")
					.digest(Build.SERIAL.getBytes())).toString(16);
		}
		catch (NoSuchAlgorithmException e) {
			Log.w("RecoEvent", "Could not hash the key", e);
			sKey = Build.SERIAL;
		}
		imgFileName = sKey.substring(0,8) + "." + clientTimestamp.getTime() + ".jpg";

		loc[Constants.LAT] = location.getLatitude();
		loc[Constants.LONG] = location.getLongitude();
		locAccuracy = location.getAccuracy();

		pitch = fPitch;
		azimuth = fAzimuth;

		if (machineSelectedPoi != null) {
			this.machineSelectedPoi = machineSelectedPoi.getId();
		}
		clientGeneratedUNA = sClientUna;
	}

	transient protected String poiNameForLog = "?";
	public String toStringForLog() {
		return "\"" + poiNameForLog + "\" " + loc[Constants.LAT] + ", " + loc[Constants.LONG];
	}

	protected void setUserSelectedPoi(@NonNull Poi poi) {
		userSelectedPoi = poi.getId();
		userFeedback = userSelectedPoi.equals(machineSelectedPoi);
		isIndex = poi.usig == null || poi.usig.unas == null || poi.usig.unas.length == 0;
		poi.addClientGeneratedUna(azimuth, clientGeneratedUNA);
	}
}

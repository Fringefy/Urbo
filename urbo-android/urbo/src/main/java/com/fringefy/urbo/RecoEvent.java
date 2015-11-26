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
	public long getTime() {
		return clientTimestamp.getTime();
	}

	private final String machineSelectedPoi;
	public String getMachineSelectedPoiId() {
		return machineSelectedPoi;
	}
	private String userSelectedPoi = null;

	/** client location, accuracy, and camera azimuth */
	private final double[] loc = new double[] { Double.NaN, Double.NaN};
	private double locAccuracy;
	public double getCamAzimuth() { return azimuth; }
	private float pitch, azimuth;

	private final String imgFileName;
	public String getImgFileName() { return imgFileName; }

	private String clientGeneratedUNA;

	/**
	 * during a recognition event, If the UNA array in the userSelectedPoi is empty,
	 * add isIndex=true to the current recognition event
	 */
	private boolean isIndex = false;
	private boolean userFeedback = false;
	public boolean isConfirmed() {
		return userFeedback;
	}

	private final String deviceID;
	private static final String sKey;

	static {
		String key;
		try {
			key = new BigInteger(1, MessageDigest.getInstance("MD5")
					.digest(Build.SERIAL.getBytes())).toString(16);
		}
		catch (NoSuchAlgorithmException e) {
			Log.w("RecoEvent", "Could not hash the key", e);
			key = Build.SERIAL + Math.random();
		}
		sKey = key.substring(0, 8);
	}

	public static String currentImgFileName() {
		return sKey + new Date().getTime() + ".jpg";
	}

	protected RecoEvent(@NonNull Location location, float fPitch, float fAzimuth,
			  @Nullable Poi machineSelectedPoi, @Nullable String sClientUna) {

		deviceID = sKey;
		clientTimestamp = new Date(); // TODO: should be based on frame arrival
		imgFileName = sKey + "." + clientTimestamp.getTime() + ".jpg";

		loc[Constants.LAT] = location.getLatitude();
		loc[Constants.LONG] = location.getLongitude();
		locAccuracy = location.getAccuracy();

		pitch = fPitch;
		azimuth = fAzimuth;

		if (machineSelectedPoi == null) {
			this.machineSelectedPoi = null;
		}
		else {
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

		if (!poi.isLocked()) {
			approximateLaser(poi.loc);
		}
		poi.lock();
	}

	/**
	 * Extrapolates a location given starting point, distance and bearing.
	 * inspired by http://goo.gl/aWlB2C
	 */
	private void approximateLaser(double[] endPoint) {

		final double radiusEarthKilometres = 6371010;
		final double laserDistance = 15;

		double dDistRatio = laserDistance / radiusEarthKilometres;
		double dDistRatioSine = Math.sin(dDistRatio);
		double dDistRatioCosine = Math.cos(dDistRatio);

		double dStartLatRad = loc[Constants.LAT] * Math.PI / 180;
		double dStartLonRad = loc[Constants.LONG] * Math.PI / 180;

		double dStartLatCos = Math.cos(dStartLatRad);
		double dStartLatSin = Math.sin(dStartLatRad);

		double dEndLatRads = Math.asin( (dStartLatSin * dDistRatioCosine) +
				(dStartLatCos * dDistRatioSine * Math.cos(azimuth)) );

		double dEndLonRads = dStartLonRad
				+ Math.atan2(
				Math.sin(azimuth) * dDistRatioSine * dStartLatCos,
				dDistRatioCosine - dStartLatSin * Math.sin(dEndLatRads));

		endPoint[Constants.LAT] = dEndLatRads * 180 / Math.PI;
		endPoint[Constants.LONG] = dEndLonRads * 180 / Math.PI;
	}

}

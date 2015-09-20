package com.fringefy.urbo;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import android.location.Location;
import android.os.Build;
import android.util.Log;

/**
 * Represents a recognition event. This class can be serialized with GSON to comply
 * with the ODIE back-end. This is a POJO class with not interesting logic.
 */
// TODO: [AC] Review and set accessibility as required
public class RecoEvent {

	/** the client's timestamp */
	protected final Date clientTimestamp;

	protected String machineSelectedPoi;
	protected String userSelectedPoi;

	/** client location, accuracy, and camera azimuth */
	protected double[] loc;
	protected double locAccuracy;
	public double getCamAzimuth() { return azimuth; }
//	protected float[] deviceOrientation;
    float pitch, azimuth;

	private final String imgFileName;
	public String getImgFileName() { return imgFileName; }

	private String clientGeneratedUNA;
	protected void addUna(String una) {
		clientGeneratedUNA = una;
	}

	String getUnaForLocalPoi() {
		return clientGeneratedUNA;
	}

	/**
	 * during a recognition event, If the UNA array in the userSelectedPoi is empty,
	 * add isIndex=true to the current recognition event
	 */
	protected boolean isIndex = false;
	protected boolean userFeedback = true;

//	protected final long clientCalculationMillisec;
	protected final String deviceID;

    RecoEvent(Location location, float fPitch, float fAzimuth,
              Poi machineSelectedPoi, String sClientUna) {

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

//        synchronized (afOrientation) {
//            deviceOrientation = afOrientation.clone();
//        }
//        clientCalculationMillisec = clientTimestamp.getTime() - location.getTime();
    }

	transient protected String poiNameForLog = "?";
	public String toStringForLog() {
		return "\"" + poiNameForLog + "\"";
	}
}

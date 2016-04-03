package com.fringefy.urbo;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.Arrays;

class RotationSensorListener implements SensorEventListener {

	private static final String TAG = "RotationSensorListener";

	private static final int DEFAULT_ROTATION_VECTOR_LENGTH = 9;
	private static final int SAMSUNG_ROTATION_VECTOR_LENGTH = 3; // see https://groups.google.com/d/msg/android-developers/U3N9eL5BcJk/X3RbVdy2rZMJ
	private int nTrimRotationVector = DEFAULT_ROTATION_VECTOR_LENGTH;

	private Sensor sensorRotationVec;
	private Sensor sensorAccelerometer;
	private Sensor sensorMagnetic;
	private SensorManager sensorManager;

	private final int iOrientation;

	float[] faGravity = new float[]{0,0,0};
	float[] faGeomagnetic = new float[]{0,0,0};

	protected RotationSensorListener(Context context) {
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sensorRotationVec = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		if (sensorAccelerometer == null) {
			sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}
		sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (sensorMagnetic == null) {
			faGeomagnetic = new float[]{-39.0625f, -19.0625f, 27.25f};
		}
		iOrientation = context.getResources().getConfiguration().orientation;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.equals(sensorRotationVec)) {
			onOrientationChanged(computeOrientation(event.values));
			return;
		}
		else if (event.sensor.equals(sensorMagnetic)) {
			faGeomagnetic = event.values;
		}
		else if (event.sensor.equals(sensorAccelerometer)) {
			faGravity = event.values;
		}

		if (sensorRotationVec == null && faGravity != null && faGeomagnetic != null) {
			float R[] = new float[DEFAULT_ROTATION_VECTOR_LENGTH];
			if (SensorManager.getRotationMatrix(R, null, faGravity, faGeomagnetic)) {
				onOrientationChanged(computeOrientation(R));
			}
		}
	}

	private float[] computeOrientation(float[] afRotationVectorOrMatrix) {

		if (afRotationVectorOrMatrix == null) {
			return null;
		}

		// compute rotation matrix
		float[] R;
		if (afRotationVectorOrMatrix.length >= DEFAULT_ROTATION_VECTOR_LENGTH) {
			R = afRotationVectorOrMatrix; // this is actually rotation matrix
		}
		else {
			R = new float[DEFAULT_ROTATION_VECTOR_LENGTH];
			if (afRotationVectorOrMatrix.length > nTrimRotationVector) {
				afRotationVectorOrMatrix =
						Arrays.copyOf(afRotationVectorOrMatrix, nTrimRotationVector);
			}
			try {
				SensorManager.getRotationMatrixFromVector(R, afRotationVectorOrMatrix);
			}
			catch (java.lang.IllegalArgumentException ex) {
				Log.e(TAG, "exception in getRotationMatrixFromVector of length "
						+ afRotationVectorOrMatrix.length, ex);

				// if this is Samsung Note 3 or similar, work around it
				// see https://groups.google.com/d/msg/android-developers/U3N9eL5BcJk/X3RbVdy2rZMJ
				if (afRotationVectorOrMatrix.length > SAMSUNG_ROTATION_VECTOR_LENGTH) {
					nTrimRotationVector = SAMSUNG_ROTATION_VECTOR_LENGTH;
					return computeOrientation(Arrays.copyOf(
							afRotationVectorOrMatrix, SAMSUNG_ROTATION_VECTOR_LENGTH));
				}
				else {
					return new float[] { 0, -(float)Math.PI/2, 0 };
				}
			}
		}

		float[] afOrientation = SensorManager.getOrientation(R, new float[3]);

		// compute azimuth (REF: http://goo.gl/JWhks6)
		afOrientation[0] = (float)Math.atan2(R[1] - R[3], R[0] + R[4]);
		return afOrientation;
	}

	protected void onOrientationChanged(float[] afOrientation) {

		// heading - degrees [0, 359.99] with 0 north,
		// pitch - degrees [-180, 179.99] with 0 perpendicular upright,
		//         90 flat up, 90 flat down, -180 ~= 179 = perpendicular upside down

		// afOrientation[0] is from -PI to PI with 0 to North
		double heading = afOrientation[0] / Math.PI * 180;

		if (iOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			// afOrientation[2] == -pi/2 --> straight
			// afOrientation[2] == 0 --> down/flat
			// afOrientation[1] == -pi --> up
			Pexeso.pushPitch(-(90 + afOrientation[2]/ (float) Math.PI * 180));
		}
		else { // tuned for phone in Portrait orientation
			// afOrinetation[1] is from 0 (flat) to PI (upright)
			// afOrinetation[2] is ~0 when screen looks up, ~PI when screen looks down
			if (Math.abs(afOrientation[2]) < Math.PI/2) {
				Pexeso.pushPitch(90 + afOrientation[1] / (float) Math.PI * 180);
			}
			else {
				Pexeso.pushPitch(-(90 + afOrientation[1] / (float) Math.PI * 180));
			}
		}

		if (sensorMagnetic == null) {
			heading += SystemClock.uptimeMillis()/500;
			while (heading > 180) {
				heading -= 360;
			}
		}
		Pexeso.pushHeading((float)heading);
	}

	protected void freeze() {
		sensorManager.unregisterListener(this);
	}

	protected void unFreeze() {
		if (sensorRotationVec != null) {
			sensorManager.registerListener(this, sensorRotationVec,
					SensorManager.SENSOR_DELAY_GAME);
		}
		if (sensorAccelerometer != null) {
			sensorManager.registerListener(this, sensorAccelerometer,
					SensorManager.SENSOR_DELAY_GAME);
		}
		if (sensorMagnetic != null) {
			sensorManager.registerListener(this, sensorMagnetic,
					SensorManager.SENSOR_DELAY_GAME);
		}
	}
}

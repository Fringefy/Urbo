package com.fringefy.urbo;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.List;


@SuppressWarnings("deprecation")
public class CameraView extends SurfaceView {

	private static final String TAG = "CameraView";
	private static final double ASPECT_TOLERANCE = 0.1;	// when choosing preview size
	public static final int DEFAULT_ROTATION_VECTOR_LENGTH = 9;
	public static final int SAMSUNG_ROTATION_VECTOR_LENGTH = 3; // see https://groups.google.com/d/msg/android-developers/U3N9eL5BcJk/X3RbVdy2rZMJ
	private int nTrimRotationVector = DEFAULT_ROTATION_VECTOR_LENGTH;


// Inner Classes


// Members

	private Urbo urbo;
	private HandlerThread htCam;
	private Handler hCam;
	private boolean bCamInitialized;
	private Camera camera;

	private boolean bStartImmediately;
	private int iCamId;
	private int iFrameW, iFrameH;
	private boolean bLive;

	private Sensor sensorRotationVec;
	private Sensor sensorAccelerometer;
	private float[] mGravity;
	private Sensor sensorMagnetic;
	private float[] mGeomagnetic;

	private SensorManager sensorManager;

	private EventHandlers eventHandlers;


// Construction

	public CameraView(Context context) {
		super(context);
		init(null, 0);
	}
	
	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}
	
	public CameraView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}
	
	private void init(AttributeSet attrs, int defStyle) {
		if (isInEditMode()) {
			return;
		}

		urbo = Urbo.getInstance(getContext());

		eventHandlers = new EventHandlers();

		// initialize camera thread
		htCam = new HandlerThread("CameraThread");
		htCam.start();
		hCam = new Handler(htCam.getLooper());

		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(
			attrs, R.styleable.CameraView, defStyle, 0);

		bStartImmediately = a.getBoolean(R.styleable.CameraView_startImmediately, true);
		iCamId = a.getInt(R.styleable.CameraView_camera, 0);

		a.recycle();

		sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		sensorRotationVec = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		if (sensorRotationVec == null) {
			sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		}

		getHolder().addCallback(eventHandlers);
	}


// Public Methods

	public boolean freeze() {
		if (!bLive) {
			Log.d(TAG, "bLive = false");
			return false;
		}

		if (camera != null) {
			Log.d(TAG, "camera not null");
			camera.stopPreview();
		}
		Log.d(TAG, "Freeze()");
		sensorManager.unregisterListener(eventHandlers);

		bLive = false;
		return true;
	}

	public boolean unFreeze() {
		if (bLive) {
			Log.d(TAG, "bLive = true");
			return false;
		}

		if (camera == null) {
			bStartImmediately = true;
			return false;
		}

		camera.setPreviewCallbackWithBuffer(eventHandlers);
		camera.startPreview();

		if (sensorRotationVec != null) {
			sensorManager.registerListener(eventHandlers, sensorRotationVec,
					SensorManager.SENSOR_DELAY_GAME);
		}
		else {
			sensorManager.registerListener(eventHandlers, sensorAccelerometer,
					SensorManager.SENSOR_DELAY_GAME);
			sensorManager.registerListener(eventHandlers, sensorMagnetic,
					SensorManager.SENSOR_DELAY_GAME);
		}

		urbo.start();
		bLive = true;
		return true;
	}

	public boolean isLive() {
		return bLive || (!bCamInitialized && bStartImmediately);
	}

// Event Handlers

	private class EventHandlers implements SurfaceHolder.Callback,
			SensorEventListener, Camera.PreviewCallback {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// init camera
			hCam.post(new Runnable() {
				@Override
				public void run() {
					try {
						camera = Camera.open(iCamId);
					}
					catch (Exception e) {
						urbo.onError(TAG, "Failed to open camera " + iCamId, e);
					}
				}
			});
		}

		// finalize the camera init now that we know preview size
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, final int w, final int h) {

			hCam.post(new Runnable() {
				@Override
				public void run() {
					if (bCamInitialized) {
						Log.d(TAG, "bCamInitialized = true");
						return;
					}

					cameraSetup(w, h);
					bCamInitialized = true;

					Pexeso.initLiveFeed(iFrameW, iFrameH, camera);

					if (bStartImmediately) {
						unFreeze();
					}
				}
			});
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			freeze();

			if (camera != null) {
				camera.release();
				camera = null;
				bCamInitialized = false;
			}
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
				mGeomagnetic = event.values;
			}
			else if (event.sensor.equals(sensorAccelerometer)) {
				mGravity = event.values;
				if (sensorMagnetic == null) {
					mGeomagnetic = new float[]{-39.0625f, -19.0625f, 27.25f};
				}
			}
			// TODO: may be necessary to improve buffering of raw sensor data
			if (mGravity != null && mGeomagnetic != null) {
				float R[] = new float[DEFAULT_ROTATION_VECTOR_LENGTH];
				if (SensorManager.getRotationMatrix(R, null, mGravity, mGeomagnetic)) {
					onOrientationChanged(computeOrientation(R));
				}
			}
		}

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			Pexeso.pushFrame(data);
		}
	}


// Private Methods

	private void cameraSetup(int w, int h) {

		try {
			Camera.Parameters params = camera.getParameters();

			if (params.getSupportedFocusModes().
					contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			}
			// set the camera focus at infinity so user doesn't need to wait for focus
			else if (params.getSupportedFocusModes().
					contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
			}

			// get the optimal preview size for 90° rotation
			Camera.Size optimalSize = getOptimalSize(params.getSupportedPreviewSizes(), h, w);

			// finalize the preview settings
			params.setPreviewSize(optimalSize.width, optimalSize.height);
			iFrameW = optimalSize.width;
			iFrameH = optimalSize.height;

			// realign the CameraView to preserve the true aspect ration on screen
			// expect the parent of our CameraView to be a FrameLayout
			final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
			double cameraAspectRatio = ((double) optimalSize.width) / optimalSize.height;

			if ((((double)h) / w) > cameraAspectRatio) {
				lp.width = (int) (h / cameraAspectRatio + 0.5);
				lp.height = h;
			} else {
				lp.height = (int) (w * cameraAspectRatio + 0.5);
				lp.width = w;
				lp.topMargin = (h - lp.height) / 2;
			}
			lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;

			post(new Runnable() {
				@Override
				public void run() {
					setLayoutParams(lp);
					requestLayout();
				}
			});

			// still picture settings - be close to preview size
			Camera.Size optimalPictureSize = getOptimalSize(params.getSupportedPictureSizes(),
					optimalSize.width, optimalSize.height);
			params.setPictureSize(optimalPictureSize.width, optimalPictureSize.height);

			camera.setParameters(params);
			camera.setDisplayOrientation(90);
			camera.setPreviewDisplay(getHolder());

		} catch (Exception e) {
			urbo.onError(TAG, "Failed to finalize camera setup", e);
		}
	}

	private Camera.Size getOptimalSize(List<Camera.Size> lstSizes,
									   int iTargetW, int iTargetH) {

		double dTargetRatio = (double)iTargetW / iTargetH;
		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		for (Camera.Size size : lstSizes) {
			double dRatio = (double) size.width / size.height;

			if (Math.abs(dRatio - dTargetRatio) <= ASPECT_TOLERANCE &&
					Math.abs(size.height - iTargetH) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - iTargetH);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : lstSizes) {
				if (Math.abs(size.height - iTargetH) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - iTargetH);
				}
			}
		}

		return optimalSize;
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
					return computeOrientation(Arrays.copyOf(afRotationVectorOrMatrix, SAMSUNG_ROTATION_VECTOR_LENGTH));
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

	private void onOrientationChanged(float[] afOrientation) {

		// heading - degrees [0, 359.99] with 0 north,
		// pitch - degrees [-180, 179.99] with 0 perpendicular upright,
		//         90 flat up, 90 flat down, -180 ~= 179 = perpendicular upside down

		// afOrientation[0] is from -PI to PI with 0 to North
		double heading = afOrientation[0] / Math.PI * 180;
		if (heading < 0) {
			heading += 360;
		}

		// afOrinetation[1] is from 0 (flat) to PI (upright)
		// afOrinetation[2] is ~0 when screen looks up, ~PI when screen looks down
		if (Math.abs(afOrientation[2]) < Math.PI/2) {
			Pexeso.pushPitch(90 + afOrientation[1] / (float) Math.PI * 180);
		}
		else {
			Pexeso.pushPitch(-(90 + afOrientation[1] / (float) Math.PI * 180));
		}
		Pexeso.pushHeading((float)heading);
	}


}

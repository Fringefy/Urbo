package com.fringefy.urbo;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.util.List;


@SuppressWarnings("deprecation")
public class CameraView extends SurfaceView
		implements SurfaceHolder.Callback, Camera.PreviewCallback {

	private static final String TAG = "CameraView";
	private static final double ASPECT_TOLERANCE = 0.1;	// when choosing preview size
	private static final int ROTATION_0 = 0;
	private static final int ROTATION_90 = 1;
	private static final int ROTATION_270 = -1;

// Inner Classes


// Members

	private HandlerThread htCam;
	private Handler hCam;
	private boolean bCamInitialized;
	private Camera camera;

	private boolean bStartImmediately;
	private int iCamId;
	private int iFrameW, iFrameH;
	private int iRotation;
	private boolean bLive;

	private RotationSensorListener rotationSensorListener;


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

		if (getContext().getResources().getConfiguration().orientation ==
				Configuration.ORIENTATION_LANDSCAPE) {
			iRotation = ROTATION_0;
		}
		else {
			android.hardware.Camera.CameraInfo info =
					new android.hardware.Camera.CameraInfo();
			android.hardware.Camera.getCameraInfo(0, info);
			if (info.orientation == 90) {
				iRotation = ROTATION_90;
			}
			else if (info.orientation == 270) {
				iRotation = ROTATION_270;
			}
		}

		rotationSensorListener = new RotationSensorListener(getContext());

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

		getHolder().addCallback(this);
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
		rotationSensorListener.freeze();
		Urbo.getInstance().stop();

		bLive = false;
		return true;
	}

	public boolean unFreeze() {

		if (camera == null) {
			bStartImmediately = true;
			return false;
		}

		if (bLive) {
			Log.d(TAG, "bLive = true");
			rotationSensorListener.unFreeze();
			Urbo.getInstance().start();
			return false;
		}

		camera.setPreviewCallbackWithBuffer(this);
		camera.startPreview();

		rotationSensorListener.unFreeze();
		Urbo.getInstance().start();

		bLive = true;
		return true;
	}

	public boolean isLive() {
		return bLive || (!bCamInitialized && bStartImmediately);
	}

// Event Handlers

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// init camera
			hCam.post(new Runnable() {
				@Override
				public void run() {
					try {
						camera = Camera.open(iCamId);
						Log.i(TAG, "Camera(" + iCamId + ") opened");
					}
					catch (Exception e) {
						Urbo.getInstance().onError(TAG, "Failed to open camera " + iCamId, e);
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

					Pexeso.initLiveFeed(iFrameW, iFrameH, iRotation, camera);

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
				Log.i(TAG, "Camera(" + iCamId + ") released");
				camera = null;
				bCamInitialized = false;
			}
		}

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			Pexeso.pushFrame(data);
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

			Camera.Size optimalSize;
			if (iRotation != ROTATION_0) {
				// get the optimal preview size for 90° rotation
				optimalSize = getOptimalSize(params.getSupportedPreviewSizes(), h, w);
			}
			else {
				optimalSize = getOptimalSize(params.getSupportedPreviewSizes(), w, h);
			}

			// finalize the preview settings
			params.setPreviewSize(optimalSize.width, optimalSize.height);
			iFrameW = optimalSize.width;
			iFrameH = optimalSize.height;

			// realign the CameraView to preserve the true aspect ration on screen
			// expect the parent of our CameraView to be a FrameLayout
			final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
			double cameraAspectRatio = ((double) optimalSize.width) / optimalSize.height;

			if (iRotation != ROTATION_0) {
				if ((((double) h) / w) > cameraAspectRatio) {
					lp.width = (int) (h / cameraAspectRatio + 0.5);
					lp.height = h;
				}
				else {
					lp.height = (int) (w * cameraAspectRatio + 0.5);
					lp.width = w;
					lp.topMargin = (h - lp.height) / 2;
				}
			}
			else {
				if ((((double) w) / h) > cameraAspectRatio) {
					lp.width = (int) (h * cameraAspectRatio + 0.5);
					lp.height = h;
				}
				else {
					lp.height = (int) (w / cameraAspectRatio + 0.5);
					lp.width = w;
					lp.topMargin = (h - lp.height) / 2;
				}
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

			if (iRotation == ROTATION_90) {
				camera.setDisplayOrientation(90);
			}
			else if (iRotation == ROTATION_270) {
				camera.setDisplayOrientation(270);
			}
			camera.setPreviewDisplay(getHolder());

		} catch (Exception e) {
			Urbo.getInstance().onError(TAG, "Failed to finalize camera setup", e);
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

}

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

import java.util.List;


@SuppressWarnings("deprecation")
public class CameraView extends SurfaceView {

    private static final String TAG = "CameraView";
    private static final double ASPECT_TOLERANCE = 0.1;	// when choosing preview size


// Inner Classes

    public interface Listener {
        void onStateChanged(int iStateId, Poi poi, long lSnapshotId);
        void onSnapshot(RecoEvent recoEvent);
    }


// Members

	private HandlerThread htCam;
	private Handler hCam;
	private boolean bCamInitialized;
	private Camera camera;

    private boolean bStartImmediately;
    private int iCamId;
	private int iFrameW, iFrameH;
	private boolean bLive;

	private Sensor sensorRotationVec;
	private SensorManager sensorManager;

    private Listener listener;
    private EventHandlers eventHandlers;


// Construction

	public CameraView(Context context) {
		super(context);
		init(context, null, 0);
	}
	
	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0);
	}
	
	public CameraView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}
	
	private void init(Context context, AttributeSet attrs, int defStyle) {
		if (Urbo.urbo == null) {
			throw new IllegalStateException("You must load Urbo before creating the camera view");
		}

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

		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sensorRotationVec = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

		getHolder().addCallback(eventHandlers);
	}


// Public Methods

	public boolean freeze() {
        if (!bLive) {
			Log.d(TAG, "bLive = " + String.valueOf(bLive));
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
        if (bLive){
			Log.d(TAG,"bLive = " + String.valueOf(bLive));
			return false;
		}

//		if (camera == null || !bCamInitialized) {
//			throw new IllegalStateException("Camera has not finished initializing");
//		}

        camera.setPreviewCallbackWithBuffer(eventHandlers);
		camera.startPreview();

		sensorManager.registerListener(eventHandlers, sensorRotationVec,
                SensorManager.SENSOR_DELAY_GAME);

        bLive = true;
		return true;
	}

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }


// Event Handlers

    private class EventHandlers implements SurfaceHolder.Callback,
            Camera.PreviewCallback, SensorEventListener, Pexeso.LiveFeedListener {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // init camera
            hCam.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        camera = Camera.open(iCamId);
                    } catch (Exception e) {
                        Urbo.urbo.onError(TAG, "Failed to open camera " + iCamId, e);
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
//                    if (camera != null || bCamInitialized){
//						Log.d(TAG,"camera != null or bCamInitialized = true");
//						return;
//					}

                    cameraSetup(w, h);
                    bCamInitialized = true;

                    Pexeso.initLiveFeed(w, h, eventHandlers);

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
        public void onPreviewFrame(final byte[] baImg, final Camera camera) {
            Pexeso.pushFrame(baImg);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
        /* TODO: [AC] Normalize (and smooth) heading and pitch
            heading - degrees [0, 359.99] with 0 north,
            pitch - degrees [-180, 179.99] with 0 perpendicular upright,
                90 flat up, -180 ~= 179 = perpendicular upside down
         */
            Pexeso.pushPitch(event.values[1]);
            Pexeso.pushHeading(event.values[0]);
        }

        @Override
        public void onNewBuffer(byte[] baBuf) {
            camera.addCallbackBuffer(baBuf);
        }

        @Override
        public boolean onStateChanged(int iStateId, Poi poi, long lSnapshotId) {
            if (listener != null) {
                listener.onStateChanged(iStateId, poi, lSnapshotId);
            }

            return true;
        }

        @Override
        public void onSnapshot(RecoEvent recoEvent) {
            if (listener != null) {
                listener.onSnapshot(recoEvent);
            }
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

			// TODO: [AC] is this really necessary?
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
			Urbo.urbo.onError(TAG, "Failed to finalize camera setup", e);
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

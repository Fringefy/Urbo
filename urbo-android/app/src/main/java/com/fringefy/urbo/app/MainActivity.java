package com.fringefy.urbo.app;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.fringefy.urbo.CameraView;
import com.fringefy.urbo.DebugListener;
import com.fringefy.urbo.Poi;
import com.fringefy.urbo.Snapshot;
import com.fringefy.urbo.Urbo;

import java.io.File;


public class MainActivity extends Activity implements Urbo.Listener, DebugListener, View.OnLongClickListener {

	private static final String TAG = "MainActivity";
	public static final int DOBLECLICK_MILLIS = 900;

// Members

	private Urbo urbo;
	private CameraView cameraView;
	private DebugListener debugListener;
	private ImageView tagImageView;
	private EditText poiNameView;
	private PoiListFragment searchFragment = new PoiListFragment();

	private Poi lastRecognizedPoi = null;
	private long lastRecognizedSnapshotId = Urbo.SNAPSHOT_ID_INVALID;
	private Snapshot snapshot = null;
	private AlertDialog tagDialog;

	private TextView recognizedView;
	private TextView stateView;
	private TextView scoreView;
	private AlphaAnimation fadeoutAnimation;


// Construction

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getResources().getConfiguration().orientation ==
				Configuration.ORIENTATION_LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			debugListener = this;
		}
		else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.main_activity);
		cameraView = (CameraView) findViewById(R.id.camera_view);
		if (cameraView.isLive()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		if (debugListener == null) {
			debugListener = (DebugListener) findViewById(R.id.debug_view);
		}
		else {
			recognizedView = (TextView) findViewById(R.id.recognized);
			recognizedView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
			recognizedView.setAlpha(0f);
			recognizedView.setOnLongClickListener(this);
			stateView = (TextView) findViewById(R.id.state);
			scoreView = (TextView) findViewById(R.id.score);
			stateView.setText("");
			scoreView.setText("");
			fadeoutAnimation = new AlphaAnimation(1f, 0f);
			fadeoutAnimation.setFillAfter(true);
			fadeoutAnimation.setDuration(700);
			fadeoutAnimation.setStartOffset(2000);
		}

		View dialogView = getLayoutInflater().inflate(R.layout.tag_dialog, null);
		tagDialog = new AlertDialog.Builder(MainActivity.this)
				.setView(dialogView)
				.setPositiveButton("TAG IT!", null)
				.setNegativeButton("Cancel", null)
				.setCancelable(true)
				.create();
		tagDialog.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		poiNameView = ((EditText) dialogView.findViewById(R.id.poi_name));
		poiNameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL) {
					confirmPoi();
					return true;
				}
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					confirmPoi();
					return true;
				}
				return false;
			}
		});
		tagImageView = (ImageView) dialogView.findViewById(R.id.tag_image);

		urbo = Urbo.getInstance(this)
				.setApiKey(getString(R.string.urbo_apiKey))
				.setListener(this)
				.setDebugListener(debugListener);
	}


// UI Event Handlers

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onLongClick(View view) {
		switch (view.getId()) {

		case R.id.recognized:
			urbo.confirmRecognition(lastRecognizedSnapshotId);
			recognizedView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
			return true;
		}
		return false;
	}

	public void onClick(View view) {

		switch (view.getId()) {

		case R.id.btn_menu:
			openOptionsMenu();
			break;

		case R.id.btn_search:
			showSearch();
			break;

		case R.id.btn_tag:
			if (!urbo.getSnapshot(lastRecognizedSnapshotId)) {
				debugListener.removeField("POI");
				debugListener.removeField("Snap ID");
				urbo.takeSnapshot();
			}
			break;

		default:
			break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_unfreeze:
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			cameraView.unFreeze();
			break;

		case R.id.action_freeze:
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			cameraView.freeze();
			break;

		case R.id.action_cache_refresh:
			urbo.forceCacheRefresh();
			break;

		default:
			break;
		}
		return true;
	}


// Urbo Listeners

	@Override
	public void onStateChanged(final int iStateId, final Poi poi, final long lSnapshotId) {

		switch (iStateId) {
		case Urbo.STATE_SEARCH:
			debugListener.setField("State", "seaching...");
			break;
		case Urbo.STATE_RECOGNITION:
			debugListener.setField("State", "Recognition");
			debugListener.setField("POI", poi.getName());
			debugListener.setField("Snap ID", String.valueOf(lSnapshotId));
			break;
		case Urbo.STATE_NO_RECOGNITION:
			debugListener.setField("State", "No recognition");
			break;
		case Urbo.STATE_NON_INDEXABLE:
			debugListener.setField("State", "Non indexable");
			break;
		case Urbo.STATE_BAD_ORIENTATION:
			debugListener.setField("State", "Bad orientation");
			break;
		case Urbo.STATE_MOVING:
			debugListener.setField("State", "Moving");
			break;
		default:
			break;
		}

		if (iStateId == Urbo.STATE_RECOGNITION) {
			lastRecognizedPoi = poi;
			lastRecognizedSnapshotId = lSnapshotId;
		}

		if (recognizedView != null) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (iStateId == Urbo.STATE_RECOGNITION) {
						recognizedView.clearAnimation();
						recognizedView.setText(poi.getName());
						recognizedView.setAlpha(1f);
					}
					else {
						if (recognizedView.getAnimation() == null) {
							recognizedView.startAnimation(fadeoutAnimation);
						}
					}
				}
			});
		}
	}

	@Override
	public void onSnapshot(Snapshot snapshot) {
		if (snapshot.isTagged()) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					recognizedView.clearAnimation();
					recognizedView.setText("Thanks for sharing!");
					recognizedView.setAlpha(1f);
					recognizedView.startAnimation(fadeoutAnimation);
				}
			});
			return;
		}
		this.snapshot = snapshot;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				openTagDialog();
			}
		});
	}

	private void confirmPoi() {
		Poi poi = getPoi(poiNameView.getText());
		if (poi != null && snapshot != null) {
			urbo.tagSnapshot(snapshot, poi);
			snapshot = null;
			tagDialog.dismiss();
		}
	}

	private Poi getPoi(CharSequence csPoiName) {
		String sPoiName = csPoiName.toString().trim();
		if (sPoiName.isEmpty()) {
			return null;
		}
		for (Poi poi : urbo.getPoiShortlist()) {
			if (poi.getName().equalsIgnoreCase(sPoiName)) {
				return poi;
			}
		}
		return new Poi(sPoiName).setFirstComment("Created with Urbo App");
	}

	private void openTagDialog() {
		poiNameView.setText(lastRecognizedPoi == null ? "" : lastRecognizedPoi.getName());
		tagImageView.setImageURI(
				Uri.fromFile(new File(getCacheDir(), snapshot.getImgFileName())));
		tagDialog.show();
		tagDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						confirmPoi();
					}
				});
		tagDialog.getWindow().setLayout(
				getWindow().getDecorView().getWidth() * 2 / 3,
				getWindow().getDecorView().getHeight() * 2 / 3);

		for (Urbo.PoiVote vote : snapshot.getVotes()) {
			Log.d(TAG, vote.poi.getName() + "\t" + String.format("%.1f", vote.fVote));
		}

	}

	private void showSearch() {
		searchFragment.setList(urbo.getPoiShortlist());

		getFragmentManager().beginTransaction()
				.add(R.id.activity_main, searchFragment, "searchFragment")
				.addToBackStack("searchFragment")
				.commit();
	}

	@Override
	public void toast(String sMsg) {
	}

	@Override
	public void setField(final String sId, final String sVal) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (sId.equals("score")) {
					scoreView.setText(sVal.replaceFirst("^k", ""));
				}
				else if (sId.equals("State")) {
					stateView.setText(sVal);
				}
			}
		});
	}

	@Override
	public void removeField(String sId) {
		if (sId.equals("score")) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					scoreView.setText("");
				}
			});
		}
	}
}
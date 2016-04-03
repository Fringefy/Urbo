package com.fringefy.urbo.app;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.fringefy.urbo.CameraView;
import com.fringefy.urbo.DebugListener;
import com.fringefy.urbo.Poi;
import com.fringefy.urbo.Snapshot;
import com.fringefy.urbo.Urbo;


public class MainActivity extends Activity implements Urbo.Listener {

	private static final String TAG = "Urbo";

// Members

	protected Urbo urbo;
	protected CameraView cameraView;
	private DebugListener debugListener;
	private ImageView tagImageView;
	private EditText poiNameView;
	private PoiListFragment searchFragment = new PoiListFragment();

	protected Snapshot snapshot = null;
	private AlertDialog tagDialog;


// Construction

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_activity);

		cameraView = (CameraView) findViewById(R.id.camera_view);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		debugListener = (DebugListener) findViewById(R.id.debug_view);

		View dialogView = getLayoutInflater().inflate(R.layout.tag_dialog, null);
		tagDialog = new AlertDialog.Builder(this)
			.setView(dialogView)
			.setPositiveButton("TAG IT!", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					tagSnapshot();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissSnapshot();
				}
			})
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					dismissSnapshot();
				}
			})
//	cannot use this because we set minSdkVersion lower than 17
//			.setOnDismissListener(new DialogInterface.OnDismissListener() {
//				@Override
//				public void onDismiss(DialogInterface dialog) {
//					dismissSnapshot();
//				}
//			})
			.setCancelable(true)
			.create();
		tagDialog.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		poiNameView = ((EditText) dialogView.findViewById(R.id.poi_name));
		poiNameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL) {
					tagSnapshot();
					return true;
				}
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					tagSnapshot();
					return true;
				}
				return false;
			}
		});
		tagImageView = (ImageView) dialogView.findViewById(R.id.tag_image);

		urbo = Urbo.createInstance(this, new Urbo.Params(getString(R.string.urbo_apiKey)));
		urbo.setListener(this).setDebugListener(debugListener);
	}


// UI Event Handlers

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
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
			if (snapshot == null) {
				Log.d(TAG, "takeSnapshot");
				urbo.takeSnapshot();
			}
			else {
				showDialog();
			};
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
	public void onStateChanged(int iStateId, Snapshot snapshot) {

		switch (iStateId) {
		case Urbo.STATE_SEARCH:
			debugListener.setField("State", "Searching...");
			break;
		case Urbo.STATE_RECOGNITION:
			debugListener.setField("State", "Recognition");
			if (snapshot.getPoi() == null) {
				debugListener.setField("POI", "null");
			}
			else {
				debugListener.setField("POI", snapshot.getPoi().getName());
			}
			this.snapshot = snapshot;
			break;
		case Urbo.STATE_NO_RECOGNITION:
			debugListener.setField("State", "No recognition");
			break;
		case Urbo.STATE_NON_INDEXABLE:
			debugListener.setField("State", "Not indexable");
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
	}

	@Override
	public void onSnapshot(Snapshot snapshot) {
		this.snapshot = snapshot;
		showDialog();
	}

	private void showDialog() {
		if (snapshot.getPoi() != null) {
			poiNameView.setText(snapshot.getPoi().getName());
		}
		Drawable drawable = Drawable.createFromStream(snapshot.getJpegStream(), "");
		tagImageView.setImageDrawable(drawable);
		tagImageView.setMinimumHeight(getWindow().getDecorView().getHeight() / 2);
		tagDialog.show();

		for (Urbo.PoiVote vote : snapshot.getVotes()) {
			Log.d(TAG, String.format("vote\t%.2f\t%s", vote.fVote, vote.poi.getName()));
		}
	}

	private void tagSnapshot() {
		Poi poi = getPoi(poiNameView.getText());
		if (poi != null && snapshot != null) {
			urbo.tagSnapshot(snapshot, poi);
		}
		snapshot = null;
		tagDialog.dismiss();
	}

	private void dismissSnapshot() {
		urbo.rejectRecognition(snapshot);
		snapshot = null;
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

	private void showSearch() {
		searchFragment.setList(urbo.getPoiShortlist());

		getFragmentManager().beginTransaction()
				.add(R.id.activity_main, searchFragment, "searchFragment")
				.addToBackStack("searchFragment")
				.commit();
	}
}
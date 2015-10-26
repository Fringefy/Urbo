package com.fringefy.urbo.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.fringefy.urbo.CameraView;
import com.fringefy.urbo.Poi;
import com.fringefy.urbo.Snapshot;
import com.fringefy.urbo.Urbo;
import com.fringefy.urbo.app.widget.DebugView;

public class MainActivity extends Activity implements Urbo.Listener {

    private static final String TAG = "MainActivity";

// Members

	private Urbo urbo;
	private CameraView cameraView;
	private PoiListFragment searchFragment = new PoiListFragment();
	private DebugView debugView;

	private Poi lastRecognizedPoi = null;
	private long lastRecognizedSnapshotId = -1;
	private Snapshot snapshot = null;

// Construction

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		cameraView = (CameraView) findViewById(R.id.camera_view);
		if (cameraView.isLive()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		debugView = (DebugView) findViewById(R.id.debug_view);

		urbo = Urbo.getInstance(this)
				.setListener(this)
				.setDebugListener(debugView)
				.setDisplayView((ImageView) findViewById(R.id.tag_image));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}


// UI Event Handlers

	@Override
	public void onBackPressed() {
		if (findViewById(R.id.btn_cancel).isShown()) {
			onClick(findViewById(R.id.btn_cancel));
		}
		else {
			super.onBackPressed();
		}
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
			if (urbo.getSnapshot(lastRecognizedSnapshotId)) {
				((EditText) findViewById(R.id.poi_name)).setText(lastRecognizedPoi.getName());
			}
			else {
				lastRecognizedSnapshotId = -1;
				debugView.removeField("POI");
				debugView.removeField("Snap ID");
				((EditText) findViewById(R.id.poi_name)).setText("");
				urbo.takeSnapshot();
			}
			break;

		case R.id.btn_cancel:
			findViewById(R.id.tagging_view).setVisibility(View.GONE);
			break;

		case R.id.btn_confirm:
			String sPoiName = ((EditText) findViewById(R.id.poi_name)).getText().toString();
			urbo.tagSnapshot(snapshot, getPoi(sPoiName));
			findViewById(R.id.tagging_view).setVisibility(View.GONE);
			break;

		default:
			break;
		}
	}

	private Poi getPoi(String sPoiName) {
		for (Poi poi: urbo.getPoiCache()) {
			if (poi.getName().equalsIgnoreCase(sPoiName)) {
				return poi;
			}
		}
		return new Poi(sPoiName);
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
			debugView.setField("State", "seaching...");
			break;
		case Urbo.STATE_RECOGNITION:
			debugView.setField("State", "Recognition");
			debugView.setField("POI", poi.getName());
			debugView.setField("Snap ID", String.valueOf(lSnapshotId));
			break;
		case Urbo.STATE_NO_RECOGNITION:
			debugView.setField("State", "No recognition");
			break;
		case Urbo.STATE_NON_INDEXABLE:
			debugView.setField("State", "Non indexable");
			break;
		case Urbo.STATE_BAD_ORIENTATION:
			debugView.setField("State", "Bad orientation");
			break;
		case Urbo.STATE_MOVING:
			debugView.setField("State", "Moving");
			break;
		default:
			break;
		}

		if (iStateId == Urbo.STATE_RECOGNITION) {
			lastRecognizedPoi = poi;
			lastRecognizedSnapshotId = lSnapshotId;
		}
	}

	@Override
	public void onSnapshot(Snapshot snapshot) {
		findViewById(R.id.tagging_view).setVisibility(View.VISIBLE);
		this.snapshot = snapshot;
	}

    private void showSearch() {
        searchFragment.setList(urbo.getPoiCache());

        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, searchFragment, "searchFragment")
                .addToBackStack("searchFragment")
                .commit();
    }
}
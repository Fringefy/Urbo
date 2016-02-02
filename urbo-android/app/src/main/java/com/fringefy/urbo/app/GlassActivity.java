package com.fringefy.urbo.app;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.fringefy.urbo.DebugListener;
import com.fringefy.urbo.Snapshot;
import com.fringefy.urbo.Urbo;

public class GlassActivity extends MainActivity implements DebugListener, View.OnLongClickListener {

	// an easy indicator of running on glasses is that the launcher from which our app started is LANDSCAPE

	private TextView recognizedView;
	private TextView stateView;
	private TextView scoreView;
	private AlphaAnimation fadeoutAnimation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		boolean bGlassesInterface = getResources().getConfiguration().orientation ==
				Configuration.ORIENTATION_LANDSCAPE;

		super.onCreate(savedInstanceState);

		if (!bGlassesInterface) {
			startActivity(new Intent(this, MainActivity.class));
			finish();
			return;
		}

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

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

		urbo.setDebugListener(this);
	}

	@Override
	public boolean onLongClick(View view) {
		switch (view.getId()) {

		case R.id.recognized:
			urbo.confirmRecognition(snapshot);
			recognizedView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
			recognizedView.clearAnimation();
			recognizedView.setText("Thanks for sharing!");
			recognizedView.setAlpha(1f);
			recognizedView.startAnimation(fadeoutAnimation);
			return true;
		}
		return false;
	}

	@Override
	public void onStateChanged(int iStateId, Snapshot snapshot) {
		super.onStateChanged(iStateId, snapshot);
		if (iStateId == Urbo.STATE_RECOGNITION) {
			recognizedView.clearAnimation();
			recognizedView.setText(snapshot.getPoi().getName());
			recognizedView.setAlpha(1f);
		}
		else if (recognizedView.getAnimation() == null) {
			recognizedView.startAnimation(fadeoutAnimation);
		}
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

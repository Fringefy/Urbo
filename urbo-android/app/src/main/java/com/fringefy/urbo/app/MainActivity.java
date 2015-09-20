package com.fringefy.urbo.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.fringefy.urbo.CameraView;
import com.fringefy.urbo.Pexeso;
import com.fringefy.urbo.Poi;
import com.fringefy.urbo.RecoEvent;
import com.fringefy.urbo.Urbo;


public class MainActivity extends Activity implements CameraView.Listener {

// Members

    private Urbo urbo;
    private CameraView cameraView;
    private TextView textStates;


// Construction

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        urbo = Urbo.load(this);
        setContentView(R.layout.activity_main);
        cameraView = (CameraView) findViewById(R.id.camera_view);
        cameraView.setListener(this);

        initViews();
    }

    private void initViews() {
        Button btn_menu = (Button) findViewById(R.id.btn_menu);
        btn_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOptionsMenu();
            }
        });
        textStates = (TextView) findViewById(R.id.text_states);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }


// UI Event Handlers

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_unfreeze:
                cameraView.unFreeze();
                    break;

            case R.id.action_freeze:
                cameraView.freeze();
                    break;
            default:
                break;
        }
        return true;
    }


// Urbo Listeners

    @Override
    public void onStateChanged(int iStateId, Poi poi, long lSnapshotId) {
        String poiName = "no POI";
        if (poi != null){
            poiName = poi.getName();
        }
        switch (iStateId){
            case Pexeso.STATE_SEARCH:
                textStates.setText("seaching...\n" + poiName);
                break;
            case Pexeso.STATE_RECOGNITION:
                textStates.setText("Recognition\n" + poiName);
                break;
            case Pexeso.STATE_NO_RECOGNITION:
                textStates.setText("No recognition\n" + poiName);
                break;
            case Pexeso.STATE_NON_INDEXABLE:
                textStates.setText("Non indexable\n" + poiName);
                break;
            case Pexeso.STATE_BAD_ORIENTATION:
                textStates.setText("Bad orientation\n" + poiName);
                break;
            default:
                break;
        }
    }

    @Override
    public void onSnapshot(RecoEvent recoEvent) {
    }
}
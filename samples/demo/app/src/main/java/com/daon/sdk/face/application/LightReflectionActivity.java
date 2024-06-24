package com.daon.sdk.face.application;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.daon.sdk.face.Config;
import com.daon.sdk.face.LightReflectionView;
import com.daon.sdk.face.DaonFace;
import com.daon.sdk.face.LivenessResult;
import com.daon.sdk.face.Result;
import com.daon.sdk.face.YUV;
import com.daon.sdk.face.application.camera.CameraFragment;
import com.daon.sdk.face.application.camera.CameraFragmentFactory;


public class LightReflectionActivity extends AppCompatActivity implements CameraFragment.CameraImageCallback {

    private TextView flashView;

    private Button trackerStatusButton;
    private Button positionStatusButton;

    private int brightnessMode;
    private int brightnessValue;

    private DaonFace daonFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_liveness_clr);

        if (null == savedInstanceState) {
            CameraFragment fragment = CameraFragmentFactory.getFragment(this);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.preview, fragment)
                    .commit();
        }

        LightReflectionView clrView = findViewById(R.id.sequence);

        flashView = findViewById(R.id.flashView);
        trackerStatusButton = findViewById(R.id.trackerStatusButton);
        positionStatusButton = findViewById(R.id.positionStatusButton);


        try {
            brightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
            brightnessValue = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            Log.e("DAON", "Setting not available", e);
        }

        try {
            daonFace = new DaonFace(this, DaonFace.OPTION_LIVENESS_CLR | DaonFace.OPTION_DEVICE_POSITION);
            daonFace.setLightReflectionView(clrView);
            daonFace.setConsolidateResults(false);

            Bundle cfg = new Bundle();
            cfg.putFloat(Config.CLR_PITCH_DELTA, 1.5f);
            cfg.putInt(Config.CLR_SEQUENCE_DURATION, 334);
            cfg.putInt(Config.CLR_SEQUENCE_LENGTH, 10);
            //cfg.putString(Config.CLR_SEQUENCE_COLORS, "['wh', 'bk', 'yl', 'rd', 'mg', 'rd', 'bl', 'gr', 'yl', 'rd']");
            daonFace.setConfiguration(cfg);
        } catch (Exception e) {
            Log.e("DAON", "Error initializing DaonFace", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        changeScreenBrightness(255);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        daonFace.stop();
        restoreBrightness();
    }

    @Override
    public void onImageAvailable(YUV image) {
        daonFace.analyze(image).addAnalysisListener(this::process);
    }

    private void changeScreenBrightness(int screenBrightnessValue)  {

        if (Settings.System.canWrite(this)) {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightnessValue);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Brightness");

            builder.setMessage("Allow this app to modify system settings (display brightness)");
            builder.setPositiveButton(R.string.action_settings, (dialog, which) -> {
                // If do not have write settings permission then open the Can modify system settings panel.
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                startActivity(intent);

                dialog.dismiss();
            });

            builder.setNegativeButton(R.string.cancel, null);
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(true);
            dialog.show();
        }
    }

    private void restoreBrightness() {
        if (Settings.System.canWrite(this)) {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, brightnessMode);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessValue);
        }
    }

    private String getMessage(float score) {

        if (score > -200) {

            if (score >= -12)
                return "Pass" + "(" + score + ")";
            return "Spoof" + "(" + score + ")";
        }

        if (score == -200)
            return "Failed";
        else if (score == -201)
            return "Poor tracking";
        else if (score == -202)
            return "Head too small";
        else if (score == -203)
            return"Poor face position";
        else if (score == -204)
            return "Video capture continuity error";
        else if (score == -205)
            return "Environment too bright";
        else if (score == -206)
            return "Dark frame capture bug";
        else if (score == -207)
            return "Poor framerate";
        else if (score == -208)
            return "Too much motion";
        else if (score == -209)
            return "Not enough frames";

        return "Unknown error";
    }

    private String getState(int state) {

        switch (state) {
            case LivenessResult.STATE_INIT:        return "Ready";
            case LivenessResult.STATE_TRACKING:    return "Tracking";
            case LivenessResult.STATE_ANALYZING:   return "Sequence";
            case LivenessResult.STATE_DONE:        return "Done";
            default:
                return "--";
        }
    }

    int lastState;


    private void process(Result result, YUV image) {

        if (result.hasLightReflectionData()) {

            Bundle b = result.getBundle();

            int state = b.getInt(LivenessResult.RESULT_STATE);
            if (lastState != state) {

                android.util.Log.d("DAON", "STATE: " + getState(state));
                trackerStatusButton.setText(getState(state));

                if (state == LivenessResult.STATE_ANALYZING)
                    flashView.setText(R.string.face_liveness_clr_sequence);
                else if (state == LivenessResult.STATE_TRACKING)
                    flashView.setText(R.string.face_liveness_clr_start);
                else if (state == LivenessResult.STATE_DONE) {
                    flashView.setText(getMessage(b.getFloat(LivenessResult.RESULT_SCORE)));
                    android.util.Log.d("DAON", "SCORE: " + b.getFloat(LivenessResult.RESULT_SCORE));
                }
            }

            lastState = state;
        }

        if (result.hasPositionData())
            positionStatusButton.setTextColor(result.isDeviceUpright() ? Color.GREEN : Color.RED);

        if (result.hasTrackingData()) {
            if (result.isTrackingFace())
                trackerStatusButton.setTextColor(Color.GREEN);
            else if (result.getLivenessResult().getTrackerStatus() == LivenessResult.TRACKER_FACE_REFINDING)
                trackerStatusButton.setTextColor(Color.YELLOW);
            else
                trackerStatusButton.setTextColor(Color.RED);
        }
    }


}

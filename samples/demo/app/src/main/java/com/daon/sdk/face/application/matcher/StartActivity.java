package com.daon.sdk.face.application.matcher;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.TextView;

import com.daon.sdk.face.DaonFace;
import com.daon.sdk.face.application.R;
import com.daon.sdk.face.application.UserPreferences;

public class StartActivity extends AppCompatActivity {

    Button startButton;
    Button resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_matching_start);

        UserPreferences.initialize(this);

        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            if (UserPreferences.instance().getBoolean(InstructionsActivity.SHOW_INSTRUCTIONS, true))
                startActivity(InstructionsActivity.class);
            else
                startActivity(CaptureActivity.class);
        });

        resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(v -> reset());

        TextView versionTextView = findViewById(R.id.versionTextView);
        String versionName = "";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            // NA
        }

        versionTextView.setText(versionName);
    }

    private void startActivity(Class<?> activity) {
        startActivity(new Intent(this, activity));
    }



    private void reset() {

        confirm("Reset?",
                (dialog, which) -> {
                    UserPreferences preferences = UserPreferences.instance();
                    preferences.putBoolean(InstructionsActivity.SHOW_INSTRUCTIONS, true);

                    DaonFace daonFace = null;
                    try {
                        daonFace = new DaonFace(getApplicationContext(), DaonFace.OPTION_RECOGNITION);
                        daonFace.clear();
                        daonFace.stop();
                    } catch (Exception e) {
                        message("Error", e.getLocalizedMessage());
                    }
                }, (dialog, which) -> dialog.cancel()
        );
    }

    private void confirm(String message, DialogInterface.OnClickListener accept, DialogInterface.OnClickListener cancel) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setPositiveButton(R.string.yes, accept);
        builder.setNegativeButton(R.string.no, cancel);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void message(String title, String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> finish());

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }
}

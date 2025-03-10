package com.daon.sdk.face.application.matcher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.daon.sdk.face.application.EdgeToEdgeActivity;
import com.daon.sdk.face.application.R;
import com.daon.sdk.face.application.UserPreferences;

/**
 * Warnings and instructions
 */
public class InstructionsActivity extends EdgeToEdgeActivity {

    public static final String SHOW_INSTRUCTIONS = "show.instructions";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_instructions);

        Button next = findViewById(R.id.buttonNext);
        if (next != null) {
            next.setOnClickListener(v -> {
                startActivity(CaptureActivity.class);
                finish();
            });
        }

        Button nextAndDontShowAgain = findViewById(R.id.buttonNextAndDontShowAgain);
        if (nextAndDontShowAgain != null) {
            nextAndDontShowAgain.setOnClickListener(v -> {
                UserPreferences.instance().putBoolean(SHOW_INSTRUCTIONS, false);
                startActivity(CaptureActivity.class);
                finish();
            });
        }
    }

    private void startActivity(Class<?> activity) {
        startActivity(new Intent(this, activity));
    }
}

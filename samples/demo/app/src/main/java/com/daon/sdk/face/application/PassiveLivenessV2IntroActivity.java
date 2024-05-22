package com.daon.sdk.face.application;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class PassiveLivenessV2IntroActivity extends AppCompatActivity {


    @Override
    @TargetApi(28)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_liveness_passive_intro);

        ImageView personGifImageView = findViewById(R.id.personGifImageView);
        ImageView phoneGifImageView = findViewById(R.id.phoneGifImageView);

        Glide.with(this).load(R.mipmap.liveness_intro_person).into(personGifImageView);
        Glide.with(this).load(R.mipmap.liveness_intro_phone).into(phoneGifImageView);

        Button startSessionButton = findViewById(R.id.startButton);
        startSessionButton.setOnClickListener(v -> startActivity(PassiveLivenessV2Activity.class));

        // settings are set to there default the first time the app runs (only)
        UserPreferences.initialize(this, R.xml.settings_liveness);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_liveness, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivityForResult(new Intent(this, PassiveLivenessV2SettingsActivity.class), 1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void startActivity(Class<?> activity) {
        startActivity(new Intent(this, activity));
    }
}

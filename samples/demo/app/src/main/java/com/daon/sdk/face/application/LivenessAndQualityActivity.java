package com.daon.sdk.face.application;

import android.Manifest;
import android.app.AlertDialog;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daon.sdk.face.BitmapTools;
import com.daon.sdk.face.CameraView;
import com.daon.sdk.face.Config;
import com.daon.sdk.face.DaonFace;
import com.daon.sdk.face.LivenessResult;
import com.daon.sdk.face.QualityResult;
import com.daon.sdk.face.Result;
import com.daon.sdk.face.ScoreBuffer;
import com.daon.sdk.face.YUV;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Locale;

public class LivenessAndQualityActivity extends AppCompatActivity {

	private static final int width = 640;
	private static final int height = 480;

	private static final String PREF_BLINK_THRESHOLD = "pref_blink_threshold";
	private static final String PREF_HMD_THRESHOLD = "pref_hmd_threshold";
	private static final String PREF_HMD_TIME_LIMIT_NOD = "pref_hmd_time_limit_nod";
	private static final String PREF_HMD_TIME_LIMIT_SHAKE = "pref_hmd_time_limit_shake";
	private static final String PREF_HMD_CUTOFF_VALUE = "pref_hmd_cutoff_value";
	private static final String PREF_HMD_CUTOFF_SWITCH = "pref_hmd_cutoff_switch";
	private static final String PREF_QUALITY_MEASURES = "pref_quality_measures";
	private static final String PREF_BEST_IMAGE = "pref_best_image";


	private static final int REQUEST_PERMISSIONS = 0;

	// Face SDK and camera
	private DaonFace faceSDK = null;
	protected CameraView preview;

	private boolean paused = false;
	private boolean showQualityMeasures = true;
	private boolean showBestImage = true;


	private TextView blinkTextView;
	private TextView nodTextView;
	private TextView shakeTextView;
	private TextView maskTextView;
	private ImageView bestImageView;

	private Button trackerStatusButton;
	private Button qualityStatusButton;
	private Button positionStatusButton;
	private Button centeredStatusButton;
	private Button pauseButton;

	private LinearLayout measuresLayout;

	private final Hashtable<String, TextView> measures = new Hashtable<>();

	private final Handler handler = new Handler(Looper.getMainLooper());

	// Keep images from the last 2 seconds, if possible
	//
	protected ScoreBuffer<YUV> scoreBuffer = new ScoreBuffer<>(20, 2000);

	private TrackerView tracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_quality);

		preview = new CameraView(this);

		FrameLayout layout = findViewById(R.id.preview);
		layout.addView(preview);

		blinkTextView = findViewById(R.id.blinkTextView);
		nodTextView = findViewById(R.id.nodTextView);
		shakeTextView = findViewById(R.id.shakeTextView);
		maskTextView = findViewById(R.id.maskTextView);

		bestImageView = findViewById(R.id.bestImageView);

		trackerStatusButton = findViewById(R.id.trackerStatusButton);
		qualityStatusButton = findViewById(R.id.qualityStatusButton);
		positionStatusButton = findViewById(R.id.positionStatusButton);
		centeredStatusButton = findViewById(R.id.centeredStatusButton);

		blinkTextView.setTextColor(Color.GRAY);
		nodTextView.setTextColor(Color.GRAY);
		shakeTextView.setTextColor(Color.GRAY);
		maskTextView.setTextColor(Color.GRAY);

		pauseButton = findViewById(R.id.pauseButton);
		pauseButton.setOnClickListener(v -> {

			if (paused)
				startCameraPreview();
			else
				preview.stop();

			paused = !paused;

			pauseButton.setText(paused ? "Continue" : "Pause");
		});

		// Create overlay to display quality measures
		measuresLayout = new LinearLayout(this);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT,
				0);

		measuresLayout.setOrientation(LinearLayout.VERTICAL);
		measuresLayout.setLayoutParams(layoutParams);

		layout.addView(measuresLayout);

		// Add quality measure to overlay
		addQualityMeasureToLayout(measuresLayout, QualityResult.RESULT_FACE_FOUND);
		addQualityMeasureToLayout(measuresLayout, QualityResult.RESULT_POSE_ANGLE);
		addQualityMeasureToLayout(measuresLayout, QualityResult.RESULT_EYES_FOUND);
		addQualityMeasureToLayout(measuresLayout, QualityResult.RESULT_EYES_OPEN);
		addQualityMeasureToLayout(measuresLayout, QualityResult.RESULT_EYES_DISTANCE_SCORE);
		addQualityMeasureToLayout(measuresLayout, QualityResult.RESULT_LIGHTING);

		addQualityMeasureToLayout(measuresLayout, QualityResult.RESULT_SHARPNESS);
		addQualityMeasureToLayout(measuresLayout, QualityResult.RESULT_EXPOSURE);
		addQualityMeasureToLayout(measuresLayout, QualityResult.RESULT_GRAYSCALE_DENSITY);
		addQualityMeasureToLayout(measuresLayout, QualityResult.RESULT_GLOBAL_QUALITY_SCORE);


		tracker = new TrackerView(this);
		layout.addView(tracker);

		// settings are set to there default the first time the app runs (only)
		UserPreferences.initialize(this, R.xml.settings);

		// Initialize and start the Face SDK
		try {
			initializeSDK();
		} catch (Exception e) {
			showDialog("License Error", e.getMessage());
		}

	}

	private boolean checkPermissions() {

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
            return false;
        }

        // We have permission...
		return true;
	}

	@Override
  	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] state) {
		super.onRequestPermissionsResult(requestCode, permissions, state);
		if (requestCode == REQUEST_PERMISSIONS && state[0] == PackageManager.PERMISSION_GRANTED) {
			startCameraPreview();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();

		if (id == R.id.action_settings) {
			startActivityForResult(new Intent(this, LivenessAndQualitySettingsActivity.class), 1);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 1) {
			applySettings();
		}
	}


	@Override
	protected void onResume() {
		super.onResume();

		paused = false;

		// This is called the first time the Activity is started and
		// when the screen is rotated. So watch out, since onCreate
		// may be called more than once if the display is rotated.

		if (checkPermissions())
			startCameraPreview();
	}

	@Override
	protected void onPause() {

		stop();
		super.onPause();
	}

	protected void stop() {

		preview.stop();

		if (faceSDK != null)
			faceSDK.stop();

		paused = true;
	}

	private void applySettings() {

		UserPreferences preferences = UserPreferences.instance();

		showQualityMeasures = preferences.getBoolean(PREF_QUALITY_MEASURES, true);

		showBestImage = preferences.getBoolean(PREF_BEST_IMAGE, false);
		if (showBestImage) {
			bestImageView.setVisibility(View.VISIBLE);
			new Thread(new BestImageThread()).start();
		} else {
			bestImageView.setVisibility(View.GONE);
		}

		int distCutoff = -1;

		boolean on = preferences.getBoolean(PREF_HMD_CUTOFF_SWITCH, false);
		if (on) {
			int value = Integer.parseInt(preferences.getString(PREF_HMD_CUTOFF_VALUE, Integer.toString(Config.DEFAULT_HMD_DISTANCE_CUTOFF)));
			if (value < 0)
				value = 0;
			distCutoff = value;
		}

		String motionThreshold = preferences.getString(PREF_HMD_THRESHOLD, Float.toString(Config.DEFAULT_HMD_MOTION_THRESHOLD));
		String timeLimitNod = preferences.getString(PREF_HMD_TIME_LIMIT_NOD, Integer.toString(Config.DEFAULT_HMD_TIME_LIMIT_NOD));
		String timeLimitShake = preferences.getString(PREF_HMD_TIME_LIMIT_SHAKE, Integer.toString(Config.DEFAULT_HMD_TIME_LIMIT_SHAKE));
		String blinkThreshold = preferences.getString(PREF_BLINK_THRESHOLD, Float.toString(Config.DEFAULT_BLINK_THRESHOLD));

		Bundle config = new Bundle();
		config.putInt(Config.HMD_DISTANCE_CUTOFF, distCutoff);
		config.putFloat(Config.HMD_MOTION_THRESHOLD, Float.parseFloat(motionThreshold));
		config.putInt(Config.HMD_TIME_LIMIT_NOD, Integer.parseInt(timeLimitNod));
		config.putInt(Config.HMD_TIME_LIMIT_SHAKE, Integer.parseInt(timeLimitShake));
		config.putFloat(Config.BLINK_THRESHOLD, Float.parseFloat(blinkThreshold));
		config.putFloat(Config.QUALITY_THRESHOLD_RANGE, 10.0f);

		faceSDK.setConfiguration(config);
	}

	private void addQualityMeasureToLayout(LinearLayout layout, String measure) {
		TextView view = new TextView(this);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
					LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT,
					Gravity.START);

		view.setBackgroundColor(Color.argb(90, 0, 0, 0));
		view.setLayoutParams(layoutParams);
		view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		view.setTypeface(Typeface.MONOSPACE);

		measures.put(measure, view);
		layout.addView(view);
	}

	private void updateQualityMeasures(QualityResult qr) {
		Bundle result = qr.getBundle();
		for (String key : result.keySet()) {
			TextView view = this.measures.get(key);
			if (view != null) {
				switch (key) {
					case QualityResult.RESULT_EYES_DISTANCE_SCORE: {

						int value = result.getInt(key);
						updateView(view, format(" %s: %d", key, value), qr.hasAcceptableEyeDistance());

						break;
					}
					case QualityResult.RESULT_GLOBAL_QUALITY_SCORE: {

						float value = result.getFloat(key);
						updateView(view, format(" %s: %.2f", key, value), qr.hasAcceptableQuality());

						break;
					}
					default: {

						boolean value = result.getBoolean(key);
						updateView(view, format(" %s: %s", key, value ? "Yes" : "No"), value);
						break;
					}
				}

			}
		}
	}

	private String format(String format, String key, Object value) {

		String name = key.replace("result.", "");
		name = name.replace(".", " ");
		name = name.substring(0, 1).toUpperCase() + name.substring(1);

		return String.format(Locale.US, format, name, value);
	}

	private void updateView(TextView view, String text, boolean acceptable) {
		view.setText(text);
		view.setTextColor(acceptable ? Color.WHITE : Color.RED);
	}

	private void initializeSDK() throws Exception {

		// License.
		//
		// By default the license will be read from the file license.txt in the assets folder. However,
		// the license string can also be passed to the face sdk constructor.
		String license = "{\"signature\":\"dWVBSGegPDsnVr6yN97\\/FKNRunGp0eCF2b+\\/UCEsbPAgKvEB34BqkZZ82MVptijn2CwCdMx2fZ0hY5eoVM13Zf8McwLr2B5pLHM0qrLCRjl8aO2BA+wXi1rILIsasJHzBmNyx8aBy62sF9yBooesYq36lDmNcZNGed1EkT1cYlCz\\/nMUxUvBaoW5RIzOJBe92591XchbSW5VUwZW2DHznelWkCL7ofVKC0+U0zlI685J3D21+zabN4FovxX8ZLa6ADHnyiF\\/oA97xNxaryczpev3R5g65RYvceA3v\\/Z0lu0+Jco4UVBP6Z+Ongru\\/FCp+ecvsUlw6Ccj+KzzO7RCEA==\",\"organization\":\"DAON\",\"signed\":{\"features\":[\"ALL\"],\"expiry\":\"2030-12-24 00:00:00\",\"applicationIdentifier\":\"com.daon.*\"},\"version\":\"2.1\"}";

		InputStream lic = new ByteArrayInputStream(license.getBytes());

		// NOTE. Enabling all liveness options may be a bit too much for a device.
		faceSDK = new DaonFace(this,
				DaonFace.OPTION_LIVENESS_BLINK |
						DaonFace.OPTION_LIVENESS_HMD |
						DaonFace.OPTION_QUALITY |
						DaonFace.OPTION_DEVICE_POSITION |
						DaonFace.OPTION_MASK,
						lic);

		faceSDK.setConsolidateResults(true);

		blinkTextView.setVisibility(View.VISIBLE);
		nodTextView.setVisibility(View.VISIBLE);
		shakeTextView.setVisibility(View.VISIBLE);

		applySettings();
	}

	@SuppressWarnings("deprecation")
	private void startCameraPreview() {

		if (faceSDK == null)
			return;

		// Start camera preview
		Size size = preview.start(this, width, height);

		//faceSDK.setImageSize(size.width, size.height);
		tracker.setImageSize(size.width, size.height);


		// Set preview frame callback and start collecting frames.
		// Frames are in the NV21 format YUV encoding.

		preview.setPreviewFrameCallback((data, camera) -> {
			if (data != null) {
				faceSDK.analyze(new YUV(data,size.width, size.height))
						.addAnalysisListener((result, image) -> {
							tracker.setPosition(result.getRecognitionResult().getFaceRectangle());
							update(image, result);
						});
			}
		});
	}


	private void update(YUV image, Result result) {

		final LivenessResult liveness = result.getLivenessResult();

		if (liveness.isShake())
			updateLabel(shakeTextView);

		if (liveness.isNod())
			updateLabel(nodTextView);

		if (liveness.isBlink())
			updateLabel(blinkTextView);

		if (result.getQualityResult().hasMask())
			updateLabel(maskTextView);

		updateBestImage(image, result);
		updateQualityResult(result);
		updateTrackerResult(result);

		showAlert(liveness.getAlert());
	}

	private void updateBestImage(final YUV image, Result result) {

		if (showBestImage) {

			QualityResult quality = result.getQualityResult();

			// Only add images that pass the quality check.
			// This can be tweaked according to requirements.
			if (quality.hasFace() && quality.hasAcceptableQuality()) {

				// Get the overall quality score for the frame:
				// faceImageScore = EyesFoundConfidence * EyesOpenConfidence + GlobalScore

				scoreBuffer.add(image, quality.getFaceImageScore());

			}
		}
	}

	private void updateQualityResult(Result result) {

		pauseButton.setText(paused ? "Continue" : "Pause");

		qualityStatusButton.setTextColor(result.getQualityResult().hasAcceptableQuality()
				? Color.GREEN
				: Color.RED);

		centeredStatusButton.setTextColor(result.getQualityResult().isFaceCentered()
				? Color.GREEN
				: Color.RED);

		if (!paused)
			positionStatusButton.setTextColor(result.isDeviceUpright() ? Color.GREEN : Color.RED);


		if (result.getQualityResult().hasData() && showQualityMeasures) {
			measuresLayout.setVisibility(View.VISIBLE);
			updateQualityMeasures(result.getQualityResult());
		} else {
			measuresLayout.setVisibility(View.GONE);
		}
	}

	private void updateTrackerResult(final Result result) {

		if (result.isTrackingFace())
			trackerStatusButton.setTextColor(Color.GREEN);
		else if (result.getLivenessResult().getTrackerStatus() == LivenessResult.TRACKER_FACE_REFINDING)
			trackerStatusButton.setTextColor(Color.YELLOW);
		else
			trackerStatusButton.setTextColor(Color.RED);
	}

	private void updateLabel(final TextView status) {

		if (paused)
			return;

		if (status != null) {

			status.setTextColor(getResources().getColor(R.color.colorEnabled));
			status.setBackgroundColor(Color.WHITE);

			// Reset after one second
			status.postDelayed(() -> {
				status.setTextColor(Color.GRAY);
				status.setBackgroundColor(getResources().getColor(R.color.colorBackground));
			}, 1000);
		}
	}


	private void showDialog(String title, String message) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);

		builder.setMessage(message);
		builder.setPositiveButton(R.string.ok, (dialog, which) -> finish());

		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.setCancelable(false);
		dialog.show();
	}

	private int lastAlert;

	protected void showAlert(int alert) {

		if (alert > 0 && alert != lastAlert) {

			Snackbar sb = Snackbar.make(findViewById(android.R.id.content), getMessage(alert), Snackbar.LENGTH_LONG);
			sb.show();
		}

		lastAlert = alert;
	}

	private int getMessage(int alert) {

		switch (alert) {
			case LivenessResult.ALERT_MOTION_TOO_FAST: 			return R.string.face_liveness_hmd_motion_too_fast;
			case LivenessResult.ALERT_MOTION_SWING_TOO_FAST: 	return R.string.face_liveness_hmd_motion_swing_too_fast;
			case LivenessResult.ALERT_MOTION_TOO_FAR: 			return R.string.face_liveness_hmd_motion_too_far;
			case LivenessResult.ALERT_FACE_TOO_CLOSE_TO_EDGE: 	return R.string.face_liveness_hmd_too_close_to_edge;
			case LivenessResult.ALERT_FACE_TOO_NEAR:			return R.string.face_liveness_hmd_too_near;
			case LivenessResult.ALERT_FACE_TOO_FAR:				return R.string.face_liveness_hmd_too_far;
		}

		return R.string.face_liveness_failed;
	}

	private class BestImageThread implements Runnable {
		@Override
		public void run() {
			try {
				while (showBestImage) {

					synchronized (this) {
						wait(1000);
					}

					// Get image with highest score. Rotate to portrait and mirror the image.
					final YUV best = scoreBuffer.getBest();
					if (best != null) {
						handler.post(() -> bestImageView.setImageBitmap(BitmapTools.rotate(best.toGrayscale(), preview.getDegreesToRotate(), true)));
					}
				}
			} catch (OutOfMemoryError e) {
				// This may happen on older devices depending on resolution,
				// the size of the score buffer etc.
			} catch (InterruptedException ignore) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
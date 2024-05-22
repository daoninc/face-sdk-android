package com.daon.sdk.face.application.matcher;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.daon.sdk.face.Config;
import com.daon.sdk.face.LivenessResult;
import com.daon.sdk.face.Result;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.daon.sdk.face.BitmapTools;
import com.daon.sdk.face.CameraView;
import com.daon.sdk.face.DaonFace;
import com.daon.sdk.face.YUV;
import com.daon.sdk.face.application.R;


public abstract class CaptureFaceFragment extends Fragment {

	private static final int width = 640;
	private static final int height = 480;

	public static final int NOTIFICATION_DELAY = 2000;

	private static final int REQUEST_PERMISSIONS = 0;

	public interface CaptureCallback {
		void onCaptureComplete();
		void onCaptureFailed(int resid);
	}

	public abstract void onLivenessDetected(YUV image, boolean passive, boolean blink);

	protected CameraView preview = null;
	protected TextView info = null;

	protected FrameLayout cameraOverlayLayout;
	protected ImageView overlay = null;
	protected ImageView qualityIndicator;

	protected CaptureCallback callback;

	protected DaonFace daonFace = null;
	private int options = DaonFace.OPTION_QUALITY|DaonFace.OPTION_LIVENESS|DaonFace.OPTION_LIVENESS_BLINK|DaonFace.OPTION_DEVICE_POSITION|DaonFace.OPTION_RECOGNITION;

	protected Handler handler = new Handler();

	protected void setOptions(int options) {
		this.options = options;
	}

	private boolean passiveDetected = false;
	private boolean blinkDetected = false;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		try {
			callback = (CaptureCallback) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context + " must implement CaptureCallback");
		}
	}


	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			daonFace = new DaonFace(getActivity(), options);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Bundle config = new Bundle();

		// Daon Passive Liveness V2 only
		// Minimum number of frames before attempting to analyze
		config.putInt(Config.LIVENESS_ANALYSIS_FRAME_COUNT, 5);

		daonFace.setConfiguration(config);

		// If the blink option is not set assume blink is true (detected)
        blinkDetected = !daonFace.isOptionEnabled(DaonFace.OPTION_LIVENESS_BLINK);

		// If the passive option is not set assume passive is true (detected)
        passiveDetected = !daonFace.isOptionEnabled(DaonFace.OPTION_LIVENESS);
	}

	@Override
	public void onStop() {
		super.onStop();

		if (daonFace != null)
			daonFace.stop();
	}

	private void createPreview() {
		ViewGroup layout = getActivity().findViewById(R.id.preview);
		if (layout != null) {

			// This may be called again, so clean up first
			layout.removeAllViews();

			preview = new CameraView(getActivity());

			cameraOverlayLayout = new FrameLayout(getActivity());

			FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
					LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT,
					Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

			layoutParams.setMargins(5, 5, 5, 5);
			cameraOverlayLayout.setLayoutParams(layoutParams);

			cameraOverlayLayout.addView(preview);

			int res = getResources().getIdentifier(getActivity().getPackageName() + ":mipmap/preview_overlay", null, null);
			if (res > 0) {
				overlay = new ImageView(getActivity());
				FrameLayout.LayoutParams faceCaptureOverlayLayoutParams = new FrameLayout.LayoutParams(
						LayoutParams.MATCH_PARENT,
						LayoutParams.MATCH_PARENT,
						Gravity.CENTER);

				overlay.setLayoutParams(faceCaptureOverlayLayoutParams);
				overlay.setBackgroundResource(res);

				cameraOverlayLayout.addView(overlay);
			}

			info = new TextView(getActivity());
			FrameLayout.LayoutParams faceCaptureInfoLayoutParams = new FrameLayout.LayoutParams(
					LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT,
					Gravity.CENTER_HORIZONTAL|Gravity.TOP);
			info.setPadding(10, 25, 10, 25);
			info.setLayoutParams(faceCaptureInfoLayoutParams);
			info.setBackgroundColor(Color.argb(100, 0, 0, 0));
			info.setTypeface(Typeface.DEFAULT_BOLD);
			info.setVisibility(View.GONE);

			cameraOverlayLayout.addView(info);

			qualityIndicator = new ImageView(getActivity());
			FrameLayout.LayoutParams faceCaptureIndicatorLayoutParams = new FrameLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT,
					Gravity.TOP | Gravity.END);
			qualityIndicator.setPadding(25, 25, 25, 25);
			qualityIndicator.setLayoutParams(faceCaptureIndicatorLayoutParams);
			qualityIndicator.setImageResource(R.drawable.image_quality_indicator);
			qualityIndicator.setVisibility(View.GONE);

			cameraOverlayLayout.addView(qualityIndicator);

			layout.addView(cameraOverlayLayout);
			layout.setVisibility(View.VISIBLE);
		}
	}

	public boolean checkPermissions(String permission) {

        if (getActivity().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, REQUEST_PERMISSIONS);
            return false;
        }

        // We have permission...
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_PERMISSIONS) {

			// We have requested multiple permissions, so all of them need
			// to be checked.

			if (verifyPermissions(grantResults)) {
				startCameraPreview();
			}
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	private boolean verifyPermissions(int[] grantResults) {
		if (grantResults.length < 1)
			return false;

		// Verify that each required permission has been granted, otherwise return false.
		for (int result : grantResults) {
			if (result != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	protected void showMessage(int id, boolean always) {
		if (getActivity() == null)
			return;

		handler.post(() -> {
			Snackbar sb = Snackbar.make(getActivity().findViewById(android.R.id.content),
					id,
					always ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
			sb.show();
		});

	}

	protected void showMessage(String message, boolean always) {
		if (getActivity() == null)
			return;

		if (message == null)
			return;

		handler.post(() -> {
			Snackbar sb = Snackbar.make(getActivity().findViewById(android.R.id.content),
					message,
					always ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
			sb.show();
		});
	}

	protected void vibrate() {
		Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
		if (vibrator != null && vibrator.hasVibrator())
			vibrator.vibrate(200);
	}


	@Override
	public void onResume() {
		super.onResume();

		if (checkPermissions(Manifest.permission.CAMERA))
			startCameraPreview();
	}

	@Override
	public void onPause() {
		stopCameraPreview();
		super.onPause();
	}

	@SuppressWarnings("deprecation")
	protected void setPreviewFrameCapture(boolean on) {
		if (preview == null)
			return;
		
		if (!on) {
			preview.setPreviewFrameCallbackWithBuffer(null);
		} else {

			// Start camera preview
			Camera.Size size = preview.start(getActivity(), width, height);

			preview.setPreviewFrameCallbackWithBuffer((data, camera) -> {
				if (data != null) {
					analyze(new YUV(data, size.width, size.height));
					preview.addPreviewFrameBuffer(data);
				}
			});
		}
	}

	private void analyze(YUV yuv) {
		daonFace.analyze(yuv)
				.addAlertListener(this::onAnalysisAlert)
				.addStateChangedListener(this::onAnalysisStateChanged)
				.addEventDetectedListener(this::onLivenessEventDetected);
	}

	private void onAnalysisAlert(Result result, int alert) {
		switch (alert) {
			case LivenessResult.ALERT_FACE_NOT_DETECTED:
				setInfo(R.string.face_liveness_hmd_face_not_detected, R.color.white); break;
			case LivenessResult.ALERT_FACE_NOT_CENTERED:
				setInfo(R.string.face_liveness_hmd_face_not_centered, R.color.white); break;
			case LivenessResult.ALERT_MOTION_TOO_FAST:
				setInfo(R.string.face_liveness_hmd_motion_too_fast, R.color.white); break;
			case LivenessResult.ALERT_MOTION_SWING_TOO_FAST:
				setInfo(R.string.face_liveness_hmd_motion_swing_too_fast, R.color.white); break;
			case LivenessResult.ALERT_MOTION_TOO_FAR:
				setInfo(R.string.face_liveness_hmd_motion_too_far, R.color.white); break;
			case LivenessResult.ALERT_FACE_TOO_CLOSE_TO_EDGE:
				setInfo(R.string.face_liveness_hmd_too_close_to_edge, R.color.white); break;
			case LivenessResult.ALERT_FACE_TOO_NEAR:
				setInfo(R.string.face_liveness_hmd_too_near, R.color.white); break;
			case LivenessResult.ALERT_FACE_TOO_FAR:
				setInfo(R.string.face_liveness_hmd_too_far, R.color.white); break;
			case LivenessResult.ALERT_LIVENESS_SPOOF:
				setInfo(R.string.face_liveness_hmd_spoof, R.color.white); break;
			case LivenessResult.ALERT_INSUFFICIENT_FACE_DATA:
				setInfo(R.string.face_liveness_hmd_insufficient_face_data, R.color.white); break;
			case LivenessResult.ALERT_INSUFFICIENT_FRAME_DATA:
				setInfo(R.string.face_liveness_hmd_insufficient_frame_data, R.color.white); break;
			case LivenessResult.ALERT_FRAME_MISMATCH:
				setInfo(R.string.face_liveness_hmd_frame_mismatch, R.color.white); break;
			case LivenessResult.ALERT_NO_MOVEMENT_DETECTED:
				setInfo(R.string.face_liveness_hmd_no_movement_detected, R.color.white); break;
			case LivenessResult.ALERT_FACE_QUALITY:
				setInfo(R.string.face_liveness_hmd_quality, R.color.white); break;
			case LivenessResult.ALERT_TIMEOUT:
				setInfo(R.string.face_liveness_timeout, R.color.white); break;
			case LivenessResult.ALERT_PERFORMANCE:
				setInfo(R.string.face_liveness_performance, R.color.white); break;
		}
	}

	private void onAnalysisStateChanged(Result result, int state, YUV image) {

		switch (state) {
			case LivenessResult.STATE_INIT:
				setInfo(R.string.state_init, R.color.white);
				break;
			case LivenessResult.STATE_START:
				setInfo(R.string.state_start, R.color.white);
				break;
			case LivenessResult.STATE_TRACKING:
				setInfo(R.string.state_tracking, R.color.white);
				break;
			case LivenessResult.STATE_ANALYZING:
				setInfo(R.string.state_analyzing, R.color.white);
				break;
			case LivenessResult.STATE_DONE:
				setInfo(R.string.state_done, R.color.white);
				break;
		}
	}

	private void onLivenessEventDetected(Result result, int event, YUV image) {
		if (event == LivenessResult.EVENT_BLINK)
			blinkDetected = true;

		if (event == LivenessResult.EVENT_PASSIVE)
			passiveDetected = true;

		if (event == LivenessResult.EVENT_SPOOF) {
			stopCameraPreview();
			captureFailed(R.string.face_liveness_hmd_spoof);
		} else {
			onLivenessDetected(image, passiveDetected, blinkDetected);
		}
	}
	protected Bitmap getPortraitImage(YUV image) {
		if (image != null)
			return BitmapTools.rotate(image.toBitmap(), preview.getDegreesToRotate(), true);
		return null;
	}

	protected void setInfo(int resid, int color) {
		if (getActivity() != null) {
			info.setText(resid);
			info.setTextColor(getResources().getColor(color));
			info.setVisibility(View.VISIBLE);
		}
	}

	protected void hideInfo() {
		info.setVisibility(View.GONE);
	}

	protected void setPreviewImage(Bitmap bmp, boolean checked) {

		setPreviewFrameCapture(false);

		if (bmp != null && getActivity() != null) {
			ViewGroup layout = getActivity().findViewById(R.id.preview);
			if (layout != null) {

				ImageView photo = new ImageView(getActivity());

				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
						LayoutParams.MATCH_PARENT,
						LayoutParams.MATCH_PARENT,
						Gravity.CENTER);
				params.setMargins(5, 5, 5, 5);
				photo.setLayoutParams(params);
				photo.setScaleType(ImageView.ScaleType.FIT_XY);
				photo.setImageBitmap(bmp);

				layout.addView(photo);

				if (checked) {
					ImageView check = new ImageView(getActivity());
					params = new FrameLayout.LayoutParams(
							LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT,
							Gravity.BOTTOM | Gravity.END);
					check.setLayoutParams(params);
					check.setImageResource(R.mipmap.verified);

					layout.addView(check);
				}
			}
		}
	}

	protected void enableOverlay(boolean enable) {
		if (overlay != null)
			overlay.setVisibility(enable ? View.VISIBLE : View.GONE);
	}

	public void startCameraPreview() {

		createPreview();

		// Set preview frame callback and start collecting frames.
		// Frames are in the NV21 format YUV encoding.
		setPreviewFrameCapture(true);

		daonFace.reset();
	}

	public void stopCameraPreview() {

		if (preview != null)
			preview.stop();

		setPreviewFrameCapture(false);

		new Handler().post(() -> {
			enableOverlay(false);
		});
	}

	protected void captureComplete() {
		new Handler().postDelayed(() -> callback.onCaptureComplete(), NOTIFICATION_DELAY);
	}

	protected void captureFailed(final int resid) {
		new Handler().postDelayed(() -> callback.onCaptureFailed(resid), NOTIFICATION_DELAY);
	}
}

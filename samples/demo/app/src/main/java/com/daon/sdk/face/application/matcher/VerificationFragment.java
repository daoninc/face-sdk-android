package com.daon.sdk.face.application.matcher;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daon.sdk.face.DaonFace;
import com.daon.sdk.face.RecognitionResult;
import com.daon.sdk.face.YUV;
import com.daon.sdk.face.application.R;

public class VerificationFragment extends CaptureFaceFragment {

    public static final float THRESHOLD = 1.0f - 0.23297233f;

    public interface VerifyCallback {

        /**
         * Called when an image was verified
         */
        void onVerifySucceeded();
        void onVerifyFailed(String message);
    }

    @Override
    public void onLivenessDetected(YUV image, boolean passive, boolean blink) {

        startTimer();

        if (passive && blink) {
            vibrate();
            setInfo(R.string.face_liveness_detected, R.color.green);
            authenticate(image);
        } else if (passive) {
            setInfo(R.string.face_liveness_blink_not_detected, R.color.green);
        } else {
            setInfo(R.string.face_liveness_not_detected, R.color.green);
        }
    }

    public VerificationFragment() {

        setOptions(DaonFace.OPTION_QUALITY |
                DaonFace.OPTION_LIVENESS |
                DaonFace.OPTION_LIVENESS_BLINK |
                DaonFace.OPTION_RECOGNITION);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_verify, container, false);
    }

    // Timeout
    private static final int RECOGNITION_TIMEOUT = 15000;
    private boolean timerStarted = false;

    Runnable timeout = () -> {
        showMessage(R.string.face_verify_timeout, false);

        stopCameraPreview();
        handler.post(() -> setInfo(R.string.face_verify_timeout, R.color.red));

        captureFailed(R.string.face_verify_timeout);
    };


    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
    }

    private void startTimer() {
        if (!timerStarted) {
            timerStarted = true;
            handler.postDelayed(timeout, RECOGNITION_TIMEOUT);
        }
    }

    private void stopTimer() {
        handler.removeCallbacks(timeout);
        timerStarted = false;
    }

    public void authenticate(YUV image) {
        final Bitmap bmp = getPortraitImage(image);
        if (bmp != null) {
            setPreviewImage(bmp, false);

            verifyImage(bmp, new VerifyCallback() {
                @Override
                public void onVerifySucceeded() {
                    stopTimer();
                    setPreviewImage(bmp, true);
                    showMessage(R.string.face_verify_complete, false);
                    captureComplete();
                }

                @Override
                public void onVerifyFailed(String message) {
                    startCameraPreview();
                    showMessage(message, false);
                }
            });
        } else {
            stopCameraPreview();
            showMessage(R.string.error_verify, false);
            captureFailed(R.string.error_verify);
        }
    }

    private void verifyImage(Bitmap bmp, final VerifyCallback callback) {

        // Check if we are we using client side recognition
        if ((daonFace.getOptions() & DaonFace.OPTION_RECOGNITION) == DaonFace.OPTION_RECOGNITION) {

            new Thread(() -> {
                RecognitionResult res = daonFace.recognize(bmp);

                Log.d("DAON", "Client based verification: " + res.getScore());

                handler.post(() -> {
                    if (res.getScore() >= THRESHOLD)
                        callback.onVerifySucceeded();
                    else
                        callback.onVerifyFailed(res.getMessage());
                });
            }).start();
        } else {

            // Make a fake server call

            BusyIndicator.getInstance().setBusy(getActivity());

            // Simulate server call
            handler.postDelayed(() -> {

                BusyIndicator.getInstance().setNotBusy(getActivity());

                // In our sample it always succeeds...
                //noinspection ConstantIfStatement
                if (true)
                    callback.onVerifySucceeded();
                else
                    callback.onVerifyFailed(getResources().getString(R.string.face_verify_failed));
            }, 2000);

        }
    }


}

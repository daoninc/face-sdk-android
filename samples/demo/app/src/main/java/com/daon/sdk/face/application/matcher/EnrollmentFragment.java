package com.daon.sdk.face.application.matcher;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.daon.sdk.face.DaonFace;
import com.daon.sdk.face.EnrollResult;
import com.daon.sdk.face.YUV;
import com.daon.sdk.face.application.R;


public class EnrollmentFragment extends CaptureFaceFragment {

    private YUV image;

    private Button takePhotoButton;
    private Button enrollButton;


    public interface EnrollCallback {

        /**
         * Called when an image is enrolled
         */
        void onEnrollSucceeded();
        void onEnrollFailed(String message);
    }

    @Override
    public void onLivenessDetected(YUV image, boolean passive, boolean blink) {
        if (passive && blink) {
            vibrate();
            hideInfo();
            this.image = image;
            takePhoto();
        } else if (passive) {
            setInfo(R.string.face_liveness_blink_not_detected, R.color.colorEnabled);
        }
    }

    public EnrollmentFragment() {

        // Add blink detection, DaonFace.OPTION_LIVENESS_BLINK, if so desired
        
        setOptions(DaonFace.OPTION_QUALITY |
                DaonFace.OPTION_LIVENESS |
                DaonFace.OPTION_RECOGNITION);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enroll, container, false);

        takePhotoButton = view.findViewById(R.id.takePhotoButton);
        takePhotoButton.setOnClickListener(v -> takePhoto());

        enrollButton = view.findViewById(R.id.doneButton);
        enrollButton.setOnClickListener(v -> enroll());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        showButtons(false);
    }

    private void takePhoto() {
        if (preview.isStopped()) {
            startCameraPreview();
            showButtons(false);
        } else {
            stopCameraPreview();
            showButtons(true);
            setPreviewImage(getPortraitImage(image), false);
        }
    }

    protected void retakePhoto() {
        handler.postDelayed(this::takePhoto, 500);
    }

    private void showButtons(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;

        if (enrollButton != null)
            enrollButton.setVisibility(visibility);

        if (takePhotoButton != null)
            takePhotoButton.setVisibility(visibility);
    }

    protected void enroll()  {
        showMessage(getResources().getString(R.string.face_enroll), false);

        handler.postDelayed(() -> showButtons(false), 500);

        final Bitmap bmp = getPortraitImage(image);
        if (bmp != null) {
            enrollImage(bmp, new EnrollCallback() {
                @Override
                public void onEnrollSucceeded() {
                    stopCameraPreview();
                    setPreviewImage(bmp, true);
                    showMessage(R.string.face_enroll_complete, false);
                    captureComplete();
                }
                @Override
                public void onEnrollFailed(String msg) {
                    showMessage(msg, false);
                    retakePhoto();
                }
            });
        } else {
            stopCameraPreview();
            showMessage(R.string.error_enroll, false);
            captureFailed(R.string.error_enroll);
        }
    }

    private void enrollImage(Bitmap bmp, final EnrollCallback callback) {
        // Check if we are we using client side recognition
        if ((daonFace.getOptions() & DaonFace.OPTION_RECOGNITION) == DaonFace.OPTION_RECOGNITION) {

            Log.d("DAON", "Client based enrollment");

            new Thread(() -> {
                EnrollResult er = daonFace.enroll(bmp);
                handler.post(() -> {
                    if (er.isEnrolled())
                        callback.onEnrollSucceeded();
                    else
                        callback.onEnrollFailed(er.getMessage());
                });
            }).start();

        } else {
            // Simulate server call
            BusyIndicator.getInstance().setBusy(getActivity());

            handler.postDelayed(() -> {
                BusyIndicator.getInstance().setNotBusy(getActivity());

                if (true)
                    callback.onEnrollSucceeded();
                else
                    callback.onEnrollFailed(getResources().getString(R.string.face_enroll_failed));
            }, 2000);
        }
    }

}

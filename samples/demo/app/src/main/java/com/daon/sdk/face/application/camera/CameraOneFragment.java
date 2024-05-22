package com.daon.sdk.face.application.camera;

import android.hardware.Camera;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.daon.sdk.face.CameraView;
import com.daon.sdk.face.YUV;
import com.daon.sdk.face.application.R;

public class CameraOneFragment extends CameraFragment {

    protected CameraView preview;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_camera_legacy, container, false);


        preview = view.findViewById(R.id.preview);
        return view;
    }


    @Override
    public void onPause() {
        super.onPause();

        preview.stop();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (hasCameraPermissions())
            startCameraPreview();
    }

    @Override
    public void onCameraPermissionsGranted() {
        startCameraPreview();
    }

    private void startCameraPreview() {

        // Start camera preview
        Camera.Size size = preview.start(getActivity(), 640, 480);

        // Add delay to make sure the camera is ready. This should avoid dark frames
        // returned by some devices.
        new Handler().postDelayed(() -> {
            preview.setPreviewFrameCallbackWithBuffer((data, camera) -> {
                if (data != null) {

                    callback.onImageAvailable(new YUV(data, size.width, size.height));
                    preview.addPreviewFrameBuffer(data);
                }
            });
        }, 500);
    }

}

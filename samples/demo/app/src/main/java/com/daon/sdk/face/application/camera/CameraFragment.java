package com.daon.sdk.face.application.camera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.daon.sdk.face.YUV;
import com.daon.sdk.face.application.R;



public abstract class CameraFragment
        extends Fragment
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String FRAGMENT_DIALOG = "dialog";
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    public interface CameraImageCallback {

        /**
         * Called when an image is available
         *
         * @param image  the image.
         */
        void onImageAvailable(YUV image);
    }

    protected CameraImageCallback callback;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            callback = (CameraImageCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement CameraImageCallback");
        }
    }

    protected boolean hasCameraPermissions() {

        if (getActivity() != null && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return false;
        }

        // We have permission...
        return true;
    }

    protected void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showCameraPermissionConfirmationDialog();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showErrorMessage(getString(R.string.request_permission));

            } else {
                onCameraPermissionsGranted();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void onCameraPermissionsGranted() {
    }

    public void showCameraPermissionConfirmationDialog() {
        new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
    }

    public void showErrorMessage(String message) {
        ErrorDialog.newInstance(getString(R.string.request_permission)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
    }
    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            //noinspection ConstantConditions
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        if (activity != null)
                            activity.finish();
                    })
                    .create();
        }
    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (parent != null)
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    })
                    .setNegativeButton(android.R.string.cancel,
                            (dialog, which) -> {
                                if (parent != null) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

}


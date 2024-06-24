package com.daon.sdk.face.application.camera;

import android.content.Context;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;
import android.util.Log;

import com.daon.sdk.face.CameraTools;


public class CameraFragmentFactory {

    private static final boolean useCameraX = true;

    public static CameraFragment getFragment(Context context) {
        if (useLegacyCameraAPI(context)) {
            Log.d("DAON", "Legacy Camera API");
            return new CameraOneFragment();
        }

       return useCameraX ? new CameraXFragment() : new CameraTwoFragment();
    }

    private static boolean useLegacyCameraAPI(Context context) {
        int level = CameraTools.getHardwareSupportLevel(context, true);
        return level == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY ||
                Build.MODEL.startsWith("Nokia 6.1") ||
                Build.MODEL.startsWith("SM-A236");

    }
}

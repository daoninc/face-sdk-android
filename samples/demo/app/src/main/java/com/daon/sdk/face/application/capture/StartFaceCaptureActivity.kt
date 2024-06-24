package com.daon.sdk.face.application.capture

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.daon.sdk.face.application.R

class StartFaceCaptureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isCameraPermissionGranted()) {
            launchCaptureSelectorActivity()
            return
        }

        setContentView(R.layout.activity_capture_permissions_request)

        findViewById<Button>(R.id.requestPermissionButton).setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA), PERMISSION_CAMERA_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_CAMERA_REQUEST && isCameraPermissionGranted()) {
            launchCaptureSelectorActivity()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(baseContext, CAMERA) == PERMISSION_GRANTED
    }

    private fun launchCaptureSelectorActivity() {
        val intent = Intent(this, CaptureSelectorActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val PERMISSION_CAMERA_REQUEST = 1
    }
}

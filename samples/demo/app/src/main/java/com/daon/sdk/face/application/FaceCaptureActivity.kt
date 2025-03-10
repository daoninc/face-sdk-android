package com.daon.sdk.face.application

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import com.daon.sdk.face.BitmapTools
import com.daon.sdk.face.DaonFace
import com.daon.sdk.face.LivenessResult
import com.daon.sdk.face.Result
import com.daon.sdk.face.YUV
import com.daon.sdk.face.application.camera.CameraFragment
import com.daon.sdk.face.application.camera.CameraFragmentFactory
import com.daon.sdk.face.application.databinding.ActivityFaceCaptureBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FaceCaptureActivity : EdgeToEdgeActivity(), CameraFragment.CameraImageCallback {

    private var fragment: CameraFragment? = null
    private lateinit var daonFace: DaonFace
    private var frames = 0
    private var image : YUV? = null
    private var lock = Object()
    private lateinit var binding: ActivityFaceCaptureBinding

    private val liveness = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFaceCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val options = if (liveness) DaonFace.OPTION_LIVENESS_V2 else DaonFace.OPTION_QUALITY or DaonFace.OPTION_DEVICE_POSITION

        try {
            daonFace = DaonFace(this, options)
            captureFace()
        } catch (e: Exception) {
            showMessage(e.localizedMessage)
        }
    }

    private fun captureFace() {

        val context = this

        CoroutineScope(Dispatchers.Default).launch {
            val image = capture()

            val bmp = image?.getDisplayBitmap(context)
            val jpg = BitmapTools.compress(bmp, 100)

            withContext(Dispatchers.Main) {
                showImage(bmp!!)
            }

            Log.d("DAON", "image size: ${jpg?.size}")
        }
    }

    private fun retry() {
        synchronized(lock) {
            frames = 0
            image = null
            daonFace.reset()
            captureFace()
        }
    }

    override fun onImageAvailable(image: YUV?) {
        image?.let {
            if (liveness) {
                analyzeWithLiveness(it)
            } else {
                analyze(it)
            }
        }
    }

    private fun analyze(yuv: YUV) {
        daonFace.analyze(yuv).addAnalysisListener{ result, img ->

            if (result.isTrackingFace) {
                if (isQuality(result)) {
                    synchronized(lock) {
                        frames += 1
                        if (frames > 10) {
                            image = img
                            lock.notify()
                        }
                    }
                } else {
                    binding.infoView.text = getQualityMessage(result)
                }
            } else {
                synchronized(lock) {
                    frames = 0
                    image = null
                }
            }
        }
    }

    private fun analyzeWithLiveness(yuv: YUV) {
        daonFace.analyze(yuv)
            .addAlertListener{ _, alert ->
                binding.infoView.setText(getAlertMessage(alert))
            }
            .addEventDetectedListener { _, event, img ->
                if (event == LivenessResult.EVENT_PASSIVE) {
                    synchronized(lock) {
                        image = img
                        lock.notify()
                    }
                }
            }
    }

    private fun isQuality(result: Result) : Boolean {
        return result.qualityResult.hasFace() &&
                result.qualityResult.hasAcceptableQuality() &&
                result.qualityResult.hasAcceptableEyeDistance() &&
                result.qualityResult.hasAcceptableSharpness() &&
                result.qualityResult.isFaceCentered &&
                result.isDeviceUpright
    }

    private fun getQualityMessage(result: Result) : String {

        if (!result.qualityResult.isFaceCentered) {
            return "Make sure face is centered"
        } else if (!result.qualityResult.hasAcceptableEyeDistance()) {
            return "Move closer"
        } else if (!result.qualityResult.hasAcceptableQuality()) {
            val goodLighting = result.qualityResult.hasAcceptableExposure() &&
                    result.qualityResult.hasUniformLighting() &&
                    result.qualityResult.hasAcceptableGrayscaleDensity()

            if (!goodLighting) {
                return "Improve lighting conditions"
            }
        }

        return "Low quality image"
    }

    private fun getAlertMessage(alert: Int) : Int {
        return when (alert) {
            LivenessResult.ALERT_FACE_NOT_DETECTED -> return R.string.face_liveness_hmd_face_not_detected
            LivenessResult.ALERT_FACE_NOT_CENTERED -> return R.string.face_liveness_hmd_face_not_centered
            LivenessResult.ALERT_MOTION_TOO_FAST -> return R.string.face_liveness_hmd_motion_too_fast
            LivenessResult.ALERT_MOTION_SWING_TOO_FAST -> return R.string.face_liveness_hmd_motion_swing_too_fast
            LivenessResult.ALERT_MOTION_TOO_FAR -> return R.string.face_liveness_hmd_motion_too_far
            LivenessResult.ALERT_FACE_TOO_CLOSE_TO_EDGE -> return R.string.face_liveness_hmd_too_close_to_edge
            LivenessResult.ALERT_FACE_TOO_NEAR -> return R.string.face_liveness_hmd_too_near
            LivenessResult.ALERT_FACE_TOO_FAR -> return R.string.face_liveness_hmd_too_far
            LivenessResult.ALERT_LIVENESS_SPOOF -> return R.string.face_liveness_hmd_spoof
            LivenessResult.ALERT_INSUFFICIENT_FACE_DATA -> return R.string.face_liveness_hmd_insufficient_face_data
            LivenessResult.ALERT_INSUFFICIENT_FRAME_DATA -> return R.string.face_liveness_hmd_insufficient_frame_data
            LivenessResult.ALERT_FRAME_MISMATCH -> return R.string.face_liveness_hmd_frame_mismatch
            LivenessResult.ALERT_NO_MOVEMENT_DETECTED -> return R.string.face_liveness_hmd_no_movement_detected
            LivenessResult.ALERT_FACE_QUALITY -> return R.string.face_liveness_hmd_quality
            LivenessResult.ALERT_TIMEOUT -> return R.string.face_liveness_timeout
            LivenessResult.ALERT_PERFORMANCE -> return R.string.face_liveness_performance
            else -> R.string.state_analyzing
        }
    }

    private fun capture() : YUV? {
        synchronized(lock) {
            while (image == null) {
                lock.wait()
            }
            return image
        }
    }

    private fun showImage(image: Bitmap) {

        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setMessage("Face captured")

        val view = ImageView(this)
        view.setImageBitmap(image)
        builder.setView(view)

        builder.setPositiveButton("Try again") { _, _ ->  retry()}

        builder.setNegativeButton("Exit") {_, _ -> finish()}

        val dialog = builder.create()
        dialog?.show()
    }

    private fun showMessage(message: String?) {
        val builder = AlertDialog.Builder(this)

        builder.setMessage(message)
        builder.setPositiveButton(R.string.ok) { _, _ -> finish() }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        showPreview()
    }

    override fun onPause() {
        super.onPause()
        removePreview()
    }

    private fun showPreview() {
        fragment = CameraFragmentFactory.getFragment(this)

        fragment?.let {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.preview, it)
                .commit()
        }
    }

    private fun removePreview() {
        fragment?.let {
            supportFragmentManager
                .beginTransaction()
                .remove(it)
                .commit()
        }
        fragment = null
    }
}
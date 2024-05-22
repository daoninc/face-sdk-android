package com.daon.sdk.face.application

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.daon.sdk.face.*
import com.daon.sdk.face.application.camera.CameraFragment
import com.daon.sdk.face.application.camera.CameraFragmentFactory
import com.daon.sdk.face.application.databinding.ActivityLivenessPassiveBlinkBinding
import com.google.android.material.snackbar.Snackbar


class PassiveAndBlinkActivity : AppCompatActivity(), CameraFragment.CameraImageCallback {

    private var fragment: CameraFragment? = null
    private var dialog: AlertDialog? = null

    private var pause = false

    private var trackingFace = false

    // Number of liveness events, e.g. blink, passive
    private var allEventsDetected = 2
    private var eventsDetected = 0

    private val sessionTimeout : Long = 7000L
    private var sessionTimer: CountDownTimer? = null
    private var sessionTimerStarted = false

    private lateinit var binding: ActivityLivenessPassiveBlinkBinding
    private lateinit var daonFace: DaonFace

    private val lock = Any()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLivenessPassiveBlinkBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        daonFace = DaonFace(this)

        // settings are set to there default the first time the app runs (only)
        UserPreferences.initialize(this, R.xml.settings_liveness_passive)
    }

    override fun onResume() {
        super.onResume()

        // Update settings
        val config = Bundle()
        val security = UserPreferences.instance().getInteger("pref_passive_security", 3)
        if (security in 1..5)
            config.putInt(Config.LIVENESS_SECURITY, security)

        daonFace.configuration = config

        retry()
    }

    override fun onPause() {
        super.onPause()

        synchronized(lock) {
            sessionTimer?.cancel()
            sessionTimerStarted = false

            dialog?.cancel()

            daonFace.stop()
            removePreview()
        }
    }

    override fun onImageAvailable(image: YUV) {

        if (pause)
            return

        daonFace.analyze(image)
            .addAnalysisListener { result, _ ->

                // If we have been tracking a face and now don't, then reset
                if (trackingFace && !result.isTrackingFace) {
                    // Not tracking face, reset liveness state
                    reset()
                } else if (isQualityImage(result)) {
                    startSessionTimer()
                }

                trackingFace = result.isTrackingFace

                val feedback = getQualityFeedback(result)
                if (feedback > 0)
                    Snackbar.make(findViewById(android.R.id.content), feedback, Snackbar.LENGTH_SHORT).show()

                // Updated tracking, quality and position info.
                updateTracking(result)
            }
            .addEventDetectedListener { _, event, img ->

                eventsDetected++

                if (event == LivenessResult.EVENT_BLINK)
                    updateLiveness(binding.blinkTextView, true)

                if (event == LivenessResult.EVENT_PASSIVE)
                    updateLiveness(binding.livenessTextView, true)

                if (event == LivenessResult.EVENT_SPOOF) {
                    updateLiveness(binding.livenessTextView, enable = false, spoof = true)
                    showMessage("Spoof", "Spoof attempt detected")

                } else if (eventsDetected == allEventsDetected) {

                    vibrate()

                    // If the user successfully passes passive liveness detection the
                    // best quality image from the frame buffer will be used to
                    // perform an authentication.
                    
                    if (img != null)
                        showMessage("Success", "Passive liveness and blink detected.", img)
                    else
                        showMessage("Image quality", "Passive liveness and blink were detected, " +
                                "but the image is not of sufficient quality. " +
                                "Please make sure to keep your face centered")
                }
            }
    }

    private fun isQualityImage(result: Result) : Boolean {
        return result.qualityResult.hasAcceptableQuality() &&
                result.qualityResult.hasAcceptableEyeDistance() &&
                result.qualityResult.isFaceCentered

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

    private fun startSessionTimer() {

        synchronized(lock) {
            if (!sessionTimerStarted) {
                sessionTimerStarted = true

                sessionTimer = object : CountDownTimer(sessionTimeout, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val seconds = (sessionTimeout - millisUntilFinished) / 1000 + 1
                        val units = "s"
                        binding.timeTextView.text = "$seconds$units"
                    }

                    override fun onFinish() {
                        binding.timeTextView.text = ""
                        showMessage("Timeout", "Liveness not detected")
                    }
                }.start()
            }
        }
    }

    private fun getQualityFeedback(result: Result): Int {

        if (!result.qualityResult.hasUniformLighting()) return R.string.face_quality_non_uniform_lighting
        else if (!result.qualityResult.hasAcceptableExposure()) return R.string.face_quality_exposure
        else if (!result.qualityResult.hasAcceptableFaceAngle()) return R.string.face_quality_pose
        else if (result.qualityResult.eyeDistance < 90) return R.string.face_quality_too_small
        else if (result.qualityResult.eyeDistance > 200) return R.string.face_quality_too_large
        else if (!result.qualityResult.hasAcceptableSharpness()) return R.string.face_quality_sharpness
        return 0
    }

    @Synchronized private fun showMessage(title: String, message: String) {
        showMessage(title, message, null)
    }

    @Synchronized private fun showMessage(title: String, message: String, image: YUV?) {

        pause = true

        sessionTimer?.cancel()

        removePreview()

        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle(title)
        builder.setMessage(message)

        if (image != null) {
            val view = ImageView(this)
            val portrait = image.getDisplayBitmap(this)

            // Check for multiple faces.
            // Note. The SDK can only detect multiple faces when doing a single image analysis.
            val res = daonFace.analyze(portrait)
            if (!res.qualityResult.hasOneFaceOnly()) {
                builder.setMessage("$message\n\nMultiple faces!\n")
            }

            view.setImageBitmap(portrait)
            builder.setView(view)
        }

        builder.setPositiveButton("Try again") { _, _ ->  retry()}

        builder.setNeutralButton("Settings") {_, _ ->
            startActivity(Intent(this, PassiveAndBlinkSettingsActivity::class.java)) }

        builder.setNegativeButton("Exit") {_, _ -> finish()}

        dialog = builder.create()
        dialog?.show()
    }

    @Synchronized private fun reset() {

        pause = false
        trackingFace = false
        eventsDetected = 0

        updateLiveness(binding.blinkTextView, false)
        updateLiveness(binding.livenessTextView, false, spoof = false)

        daonFace.reset()
    }

    private fun retry() {
        synchronized(lock) {
            sessionTimerStarted = false

            binding.timeTextView.text = ""

            reset()
            showPreview()
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator.hasVibrator())
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun updateLiveness(status: TextView?, enable: Boolean) {
        updateLiveness(status, enable, false)
    }

    private fun updateLiveness(status: TextView?, enable: Boolean, spoof: Boolean) {

        when {
            enable -> {
                status?.setTextColor(ContextCompat.getColor(this, R.color.colorEnabled))
                status?.setBackgroundColor(Color.WHITE)
            }
            spoof -> {
                status?.setTextColor(ContextCompat.getColor(this, R.color.yellow))
                status?.setBackgroundColor(Color.BLACK)
            }
            else -> {
                status?.setTextColor(Color.GRAY)
                status?.setBackgroundColor(ContextCompat.getColor(this, R.color.colorBackground))
            }
        }
    }

    private fun updateTracking(res: Result) {

        // Update face tracker status
        when {
            res.isTrackingFace -> binding.trackerStatusButton.setTextColor(Color.GREEN)
            res.livenessResult.trackerStatus == LivenessResult.TRACKER_FACE_REFINDING -> binding.trackerStatusButton.setTextColor(Color.YELLOW)
            else -> binding.trackerStatusButton.setTextColor(Color.RED)
        }

        // Update quality status
        binding.qualityStatusButton.setTextColor(if (isQualityImage(res)) Color.GREEN else Color.RED)
        binding.centeredStatusButton.setTextColor(if (res.qualityResult.isFaceCentered) Color.GREEN else Color.RED)

        // Update position
        binding.positionStatusButton.setTextColor(if (res.isDeviceUpright) Color.GREEN else Color.RED)
    }
}
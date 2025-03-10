package com.daon.sdk.face.application

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.content.FileProvider
import com.daon.sdk.face.Config
import com.daon.sdk.face.DaonFace
import com.daon.sdk.face.LivenessResult
import com.daon.sdk.face.Result
import com.daon.sdk.face.YUV
import com.daon.sdk.face.application.camera.CameraFragment
import com.daon.sdk.face.application.camera.CameraFragmentFactory
import com.daon.sdk.face.application.databinding.ActivityLivenessPassiveBinding
import java.io.File
import kotlin.math.roundToInt

// NOTE. This sample requires the Daon Face Liveness V2 library

class PassiveLivenessV2Activity : EdgeToEdgeActivity(), CameraFragment.CameraImageCallback {

    private var fragment: CameraFragment? = null
    private var holdTimer: CountDownTimer? = null

    private lateinit var binding: ActivityLivenessPassiveBinding
    private lateinit var daonFace: DaonFace

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLivenessPassiveBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val layoutParams = window.attributes
        layoutParams.screenBrightness = 1.0f
        window.attributes = layoutParams

        // settings are set to there default the first time the app runs (only)
        UserPreferences.initialize(this, R.xml.settings_liveness);

        binding.retryButton.setOnClickListener {
            showPreview()
            showMessage("")

            // Make sure we get a new countdown timer.
            stopCountDownTimer()

            daonFace.reset()
        }

        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, PassiveLivenessV2SettingsActivity::class.java)
            startActivity(intent)
        }

        try {
            // Daon Face SDK. Make sure to include the Daon Face Liveness V2 library.
            daonFace = DaonFace(this, DaonFace.OPTION_LIVENESS_V2)

        } catch (e: Exception) {
            showError(e.localizedMessage)
        }
    }

    override fun onResume() {
        super.onResume()

        val preferences = UserPreferences.instance()

        val config = Bundle()

        // Passive Liveness V2 settings
        config.putInt(Config.LIVENESS_START_DELAY, preferences.getInteger("pref_delay", 0))
        config.putInt(
            Config.LIVENESS_ANALYSIS_FRAME_COUNT,
            preferences.getInteger("pref_frames", 10)
        )
        config.putBoolean(
            Config.LIVENESS_TEMPLATE,
            preferences.getBoolean("pref_template", true)
        )
        config.putInt(Config.LIVENESS_TEMPLATE_QUALITY, 60)
        config.putBoolean(Config.LIVENESS_DEBUG, false)
        daonFace.configuration = config

        showPreview()
    }

    override fun onPause() {
        super.onPause()

        showMessage("")
        stopCountDownTimer()
        daonFace.reset()

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

        binding.retryButton.visibility = View.GONE
        binding.settingsButton.visibility = View.GONE
    }

    private fun hidePreview() {
        fragment?.let {
            supportFragmentManager
                .beginTransaction()
                .hide(it)
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
    }

    override fun onImageAvailable(image: YUV) {

        daonFace.analyze(image)
            .addAlertListener { _, alert ->

                if (alert == LivenessResult.ALERT_PERFORMANCE)
                    showSevereAlertMessage(getAlertMessage(alert))
                else
                    showAlertMessage(getAlertMessage(alert))
            }
            .addStateChangedListener { _, state, _ ->
                onAnalysisStateChanged(state)
            }
            .addEventDetectedListener { result, _, img ->
                onLivenessEventDetected(result, img)
            }
    }

    private fun onAnalysisStateChanged(state: Int) {

        when (state) {
            LivenessResult.STATE_INIT -> {
                binding.retryButton.visibility = View.GONE
                binding.settingsButton.visibility = View.GONE
                showMessage("Initializing")
            }
            LivenessResult.STATE_START -> {
                showMessage("Determining liveness")
                startCountDownTimer()
            }
            LivenessResult.STATE_TRACKING -> {
                vibrate()
                stopCountDownTimer()
                showMessage("Look alive!")
            }
            LivenessResult.STATE_ANALYZING -> {
                hidePreview()
                showMessage("Analyzing")
            }
            LivenessResult.STATE_DONE -> {
                binding.retryButton.visibility = View.VISIBLE
                binding.settingsButton.visibility = View.VISIBLE
                showMessage("Done")

            }
        }
    }

    private fun onLivenessEventDetected(result: Result, image: YUV?) {

        removePreview()

        val score = result.livenessResult.score
        if (score >= 0) {
            val percent = (score * 100).roundToInt()
            if (score > .50)
                showMessage("PASS ($percent%)", Color.GREEN, result, image)
            else
                showMessage("FAIL ($percent%)", Color.RED, result, image)
        } else {
            showMessage("Error " + score.roundToInt(), Color.RED)
        }

        val template = result.livenessResult.template
        if (template != null)
            Log.d("DAON", "Liveness Template Data: " + template.size + " bytes")
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
            else -> 0
        }
    }

    private fun showSevereAlertMessage(message: Int) {
        binding.moreInfoView.visibility = View.VISIBLE
        binding.moreInfoView.setText(message)
    }

    private fun showAlertMessage(message: Int) {
        if (message > 0) {
            binding.infoView.visibility = View.VISIBLE
            binding.infoView.setText(message)
        } else {
            binding.infoView.visibility = View.GONE
        }
    }

    private fun showMessage(message: String, color: Int = Color.WHITE) {
        Log.d("DAON", message)
        binding.infoView.visibility = if (message.isBlank()) View.GONE else View.VISIBLE
        binding.infoView.setTextColor(color)
        binding.infoView.text = message
    }

    private fun showMessage(message: String, color: Int, result: Result, image: YUV?) {

        showMessage(message, color)

        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setMessage(message)

        if (image != null) {
            val view = ImageView(this)
            view.setImageBitmap(image.getDisplayBitmap(this))
            builder.setView(view)
        }

        builder.setPositiveButton("OK") {_, _ ->}
        builder.setNegativeButton("Exit") {_, _ -> finish()}
        builder.setNeutralButton("Share") {_, _ -> share(result)}

        val dialog = builder.create()
        dialog?.show()
    }

    private fun showError(message: String?) {
        val builder = AlertDialog.Builder(this)

        builder.setMessage(message)
        builder.setPositiveButton(R.string.ok) { _, _ -> finish() }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator())
            vibrator.vibrate(200)
    }

    private fun startCountDownTimer() {

        if (holdTimer == null) {
            val duration = daonFace.configuration.getInt(Config.LIVENESS_START_DELAY, 3000)
            if (duration > 1000) { // Only show countdown if more than 1s
                holdTimer = object : CountDownTimer(duration.toLong(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val seconds = millisUntilFinished / 1000 + 1

                        showMessage("$seconds")
                    }

                    override fun onFinish() {

                    }
                }.start()
            }
        }
    }

    private fun stopCountDownTimer() {
        holdTimer?.cancel()
        holdTimer = null
    }


    private fun share(result: Result) {

        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "message/rfc822"
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Daon Passive Liveness Data")

        val file = result.bundle.getString(LivenessResult.RESULT_DEBUG)
        if (file != null) {
            val uri = FileProvider.getUriForFile(
                this,
                "com.daon.sdk.face.application.provider",
                File(file)
            )
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        } else {
            sendIntent.putExtra(Intent.EXTRA_TEXT, "No data.")
        }

        startActivity(Intent.createChooser(sendIntent, "Daon Passive Liveness Data"))
    }
}
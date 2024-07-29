package com.daon.sdk.face.application.capture

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.daon.sdk.face.application.R

class CaptureSelectorActivity : AppCompatActivity(R.layout.activity_capture_selector) {
    private var isPreviewEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<Button>(R.id.captureButton).setOnClickListener {
            startActivity(createIntent())
        }

        findViewById<Switch>(R.id.usePreviewSwitch).apply {
            isChecked = isPreviewEnabled
            setOnCheckedChangeListener { _, isChecked ->
                isPreviewEnabled = isChecked
            }
        }

        findViewById<Switch>(R.id.passiveSwitch).apply {
            isChecked = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("passive", false)
            setOnCheckedChangeListener { _, isChecked ->
                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("passive", isChecked).apply()
            }
        }

        findViewById<Switch>(R.id.blinkSwitch).apply {
            isChecked = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("blink", false)
            setOnCheckedChangeListener { _, isChecked ->
                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("blink", isChecked).apply()
            }
        }

        findViewById<Switch>(R.id.qualitySwitch).apply {
            isChecked = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("quality", false)
            setOnCheckedChangeListener { _, isChecked ->
                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("quality", isChecked).apply()
            }
        }
    }

    private fun createIntent(): Intent {
        val intent = if (isPreviewEnabled) {
            Intent(this, FaceCaptureActivity::class.java)
        } else {
            Intent(this, FaceCaptureNoUIActivity::class.java)
        }
        return intent
    }
}

package com.daon.sdk.face.application.capture

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.daon.sdk.face.FileTools
import com.daon.sdk.face.application.R
import java.io.File

class FaceCaptureNoUIActivity : AppCompatActivity(R.layout.activity_capture_no_ui) {

    private lateinit var startButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusView: TextView

    private val viewModel: FaceCaptureViewModel by viewModels {
        FaceCaptureViewModel.CameraControllerViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(viewModel)

        startButton = findViewById(R.id.startButton)
        progressBar = findViewById(R.id.progressBar)
        statusView = findViewById(R.id.statusView)

        viewModel.state.observe(this) { state ->
            state ?: return@observe
            when (state) {
                State.Idle -> {
                    startButton.isVisible = true
                    progressBar.isVisible = false
                    statusView.isVisible = false
                    statusView.text = ""
                }

                State.Detecting -> {
                    startButton.isVisible = false
                    progressBar.isVisible = false
                    statusView.isVisible = true
                }

                State.Analyzing -> {
                    progressBar.isVisible = true
                    statusView.isVisible = false
                    statusView.text = ""
                }
            }
        }

        viewModel.faceDetectionHint.observe(this) { hint ->
            hint ?: return@observe
            statusView.text = hint
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage ?: return@observe
            showError("error", errorMessage)
        }

        viewModel.result.observe(this) { data ->
            data ?: return@observe
            FileTools.write(this, "template.ifp", data)
            showMessage("Template", "${data.size} bytes")
        }

        viewModel.photo.observe(this) { bitmap ->
            bitmap ?: return@observe
            showBitmap(bitmap)
        }

        viewModel.createCameraController(applicationContext, this)

        startButton.setOnClickListener(viewModel.onStartButtonClickListener)
    }

    private fun showError(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setOnDismissListener { viewModel.state.value = State.Idle }
            .setMessage(message)
            .show()
    }

    private fun showMessage(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setOnDismissListener { viewModel.state.value = State.Idle }
            .setPositiveButton("Share") { _, _ -> share() }
            .setNegativeButton("OK", null)
            .setMessage(message)
            .show()
    }

    private fun showBitmap(bitmap: Bitmap) {

        val view = ImageView(this)
        view.setImageBitmap(bitmap)

        AlertDialog.Builder(this)
            .setTitle("Photo")
            .setOnDismissListener { viewModel.state.value = State.Idle }
            .setPositiveButton("OK", null)
            .setView(view)
            .show()
    }

    private fun share() {
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "message/rfc822"
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Daon Face SDK: Photo Data")

        val file = filesDir.absolutePath + "/template.ifp"
        val uri = FileProvider.getUriForFile(
            this,
            "com.daon.sdk.face.application.provider",
            File(file)
        )
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(sendIntent, "Daon Face SDK: Photo Data"))
    }
}
package com.daon.sdk.face.application.capture

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View.OnClickListener
import androidx.camera.view.PreviewView
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.preference.PreferenceManager
import com.daon.sdk.face.Config
import com.daon.sdk.face.LivenessResult
import com.daon.sdk.face.Result
import com.daon.sdk.face.capture.CameraController
import com.daon.sdk.face.capture.Liveness
import com.daon.sdk.face.capture.Quality

class FaceCaptureViewModel(
    app: Application,
) : AndroidViewModel(app), DefaultLifecycleObserver {

    val errorMessage = MutableLiveData<String>()
    val faceDetectionHint = MutableLiveData<String>()
    val state = MutableLiveData<State>()
    val result = MutableLiveData<ByteArray>()
    val photo = MutableLiveData<Bitmap>()


    val onStartButtonClickListener = OnClickListener {
        cameraController.startCapture()
        state.value = State.Detecting
    }

    private lateinit var cameraController: CameraController

    fun createCameraController(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {

        val builder = CameraController.Builder(context, lifecycleOwner)

        if (previewView != null)
            builder.setPreviewView(previewView)

        builder.setMedicalMaskDetection(false)
        builder.setCaptureQuality(Quality.Low)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.getBoolean("passive", false))
            builder.setPassiveLiveness(Liveness.V1)

        if (prefs.getBoolean("blink", false))
            builder.setBlinkDetection(true)

        val params = Bundle()
        params.putFloat(Config.BLINK_THRESHOLD, 0.1f)
        builder.setParameters(params)

        // Listeners
        builder.setErrorListener { exception ->
            errorMessage.postValue(exception.message)
        }.setPhotoListener { bitmap ->
            // This event is triggered when a photograph is being processed.
            // It allows you to use the photograph that has been taken, which can be
            // useful for showing a preview of the photograph taken

            state.postValue(State.Analyzing)
            photo.postValue(bitmap)
        }.setCaptureCompleteListener { data ->
            // Event that triggers after a photograph has been taken and processed.
            // The data can be submitted to the server for further processing.
            result.postValue(data)
        }.setFaceDetectionListener { result ->
            var msg = getQualityMessage(result)
            val detected = getLivenessMessage(result)
            if (detected.isNotEmpty()) {
                msg = "$msg\n\n$detected"
            }
            faceDetectionHint.postValue(msg)
        }

        cameraController = builder.build()
    }


    private fun getQualityMessage(result: Result) : String {
        if (!result.isDeviceUpright) {
            return "Hold device upright"
        } else if (result.qualityResult.hasMask()) {
            return "Remove medical mask"
        } else if (!result.qualityResult.isFaceCentered) {
            return "Keep face centered"
        } else if (!result.qualityResult.hasAcceptableEyeDistance()) {
            return "Move device closer"
        } else if (!result.qualityResult.hasAcceptableQuality()) {
            val goodLighting = result.qualityResult.hasAcceptableExposure() &&
                    result.qualityResult.hasUniformLighting() &&
                    result.qualityResult.hasAcceptableGrayscaleDensity()

            if (!goodLighting) {
                return "Improve lighting conditions"
            }

            return "Low quality image"

        } else if (result.livenessResult.alert == LivenessResult.ALERT_FACE_TOO_FAR) {
            return "Move device closer"
        } else if (result.livenessResult.alert == LivenessResult.ALERT_FACE_TOO_NEAR) {
            return "Move device further away"
        }

        return "Look alive!"
    }

    private fun getLivenessMessage(result: Result) : String {
        if (result.livenessResult.isBlink)
            return "Blink detected"
        else if (result.livenessResult.isPassive)
            return "Passive liveness detected"
        else if (result.livenessResult.spoofDetected())
            return "Spoof detected"
        return ""
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        cameraController.openCamera()
        state.value = State.Idle
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        cameraController.startPreview()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        cameraController.stopPreview()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        cameraController.closeCamera()
    }

    override fun onCleared() {
        super.onCleared()
        cameraController.close()
    }

    @Suppress("UNCHECKED_CAST")
    class CameraControllerViewModelFactory : ViewModelProvider.Factory {
        @Throws(IllegalStateException::class)
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (!modelClass.isAssignableFrom(FaceCaptureViewModel::class.java)) {
                throw IllegalStateException("Unknown class name ${modelClass.name}")
            }

            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
            return FaceCaptureViewModel(app) as T
        }
    }

}

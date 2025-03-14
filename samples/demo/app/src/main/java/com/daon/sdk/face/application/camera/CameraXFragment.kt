package com.daon.sdk.face.application.camera

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.daon.sdk.face.YUV
import com.daon.sdk.face.application.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXFragment : CameraFragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: PreviewView

    private var displayId: Int = -1
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_camera_x, container, false)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.view_finder)

        viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        viewFinder.post {

            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId

            if (hasCameraPermissions())
                bindCameraUseCases()
        }
    }

    override fun onCameraPermissionsGranted() {
        bindCameraUseCases()
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }


    /** Declare and bind preview, capture and analysis use cases */
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        val rotation = viewFinder.display.rotation

        // Bind the CameraProvider to the LifeCycleOwner
        val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({

            // CameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val resolutionSelectorBuilder = ResolutionSelector.Builder().apply {
                    setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            }

            // Preview
            preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelectorBuilder.build())
                    .setTargetRotation(rotation)
                    .build()
                    .also {
                        it.surfaceProvider = viewFinder.surfaceProvider
                    }

            // ImageAnalysis
            imageAnalyzer = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelectorBuilder.build())
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

            // Set the analyzer with the callback
            // Add delay to make sure the camera is ready. This should avoid dark frames
            // returned by some devices.
            Handler(Looper.getMainLooper()).postDelayed({
                imageAnalyzer?.setAnalyzer(cameraExecutor) { image ->

                    callback.onImageAvailable(YUV(image.image))
                    image.close()
                }
            }, 500)

            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            try {
                // A variable number of use-cases can be passed here -
                // camera provides access to CameraControl & CameraInfo
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e("DAON", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

}
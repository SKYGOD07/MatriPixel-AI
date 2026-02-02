package com.matripixel.ai.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.matripixel.ai.data.model.ColorAnalysis
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX manager for preview and image capture.
 * 
 * Lifecycle-aware implementation that properly binds/unbinds camera resources.
 */
class CameraManager(
    private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null
    
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    
    /**
     * Start camera with preview and analysis
     */
    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onAnalysis: (Bitmap, ColorAnalysis) -> Unit,
        onError: (Exception) -> Unit
    ) = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Preview
                preview = Preview.Builder()
                    .build()
                    .apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // Image Capture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                // Image Analysis
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .apply {
                        setAnalyzer(
                            analysisExecutor,
                            ConjunctivaAnalyzer(context, onAnalysis, onError)
                        )
                    }
                
                // Bind to lifecycle
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
                
                continuation.resume(Unit)
                
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
        
        continuation.invokeOnCancellation {
            stopCamera()
        }
    }
    
    /**
     * Capture image to file
     */
    suspend fun captureImage(outputFile: File): File = suspendCancellableCoroutine { continuation ->
        val imageCapture = imageCapture ?: run {
            continuation.resumeWithException(IllegalStateException("Camera not started"))
            return@suspendCancellableCoroutine
        }
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    continuation.resume(outputFile)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
    
    /**
     * Stop camera and release resources
     */
    fun stopCamera() {
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }
    
    /**
     * Check if camera is currently active
     */
    fun isCameraActive(): Boolean {
        val currentPreview = preview ?: return false
        return cameraProvider?.isBound(currentPreview) ?: false
    }
}

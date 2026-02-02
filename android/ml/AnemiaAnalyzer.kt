package com.matripixel.ai.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * AnemiaAnalyzer - CameraX Image Analyzer for Anemia Detection
 * 
 * Implements [ImageAnalysis.Analyzer] to process camera frames and detect anemia
 * from eye conjunctiva images using a TFLite model.
 * 
 * Features:
 * - YUV_420_888 to Bitmap conversion
 * - Configurable ROI (Region of Interest) for lower eyelid cropping
 * - GPU acceleration when available
 * - Non-blocking inference on analyzer thread
 * 
 * Usage:
 * ```kotlin
 * val analyzer = AnemiaAnalyzer(context) { confidence, bitmap ->
 *     // Handle result: confidence is 0.0 (healthy) to 1.0 (anemic)
 * }
 * cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis.apply {
 *     setAnalyzer(executor, analyzer)
 * })
 * ```
 *
 * @author MatriPixel AI Team
 */
class AnemiaAnalyzer(
    private val context: Context,
    private val onResult: (confidence: Float, processedBitmap: Bitmap?) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "AnemiaAnalyzer"
        
        // Model configuration
        private const val MODEL_FILENAME = "anemia_detector.tflite"
        private const val INPUT_SIZE = 224
        private const val PIXEL_SIZE = 3  // RGB
        private const val BATCH_SIZE = 1
        
        // Normalization parameters (MobileNetV3 preprocessing)
        private const val MEAN = 127.5f
        private const val STD = 127.5f
        
        // Default ROI configuration (lower eyelid region)
        // These values represent percentages of the image dimensions
        private const val DEFAULT_ROI_LEFT = 0.15f
        private const val DEFAULT_ROI_TOP = 0.55f
        private const val DEFAULT_ROI_RIGHT = 0.85f
        private const val DEFAULT_ROI_BOTTOM = 0.85f
        
        // Frame skip for performance (analyze every Nth frame)
        private const val FRAME_SKIP = 3
    }

    // TFLite interpreter
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    
    // Input/output buffers
    private val inputBuffer: ByteBuffer
    private val outputBuffer: FloatArray = FloatArray(1)
    
    // ROI configuration (can be updated dynamically)
    var roiLeft: Float = DEFAULT_ROI_LEFT
        set(value) { field = value.coerceIn(0f, 1f) }
    var roiTop: Float = DEFAULT_ROI_TOP
        set(value) { field = value.coerceIn(0f, 1f) }
    var roiRight: Float = DEFAULT_ROI_RIGHT
        set(value) { field = value.coerceIn(0f, 1f) }
    var roiBottom: Float = DEFAULT_ROI_BOTTOM
        set(value) { field = value.coerceIn(0f, 1f) }
    
    // Frame counter for skipping
    private var frameCounter = 0
    
    // Processing state
    private var isProcessing = false
    
    init {
        // Allocate input buffer
        inputBuffer = ByteBuffer.allocateDirect(
            BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE * 4  // 4 bytes per float
        ).apply {
            order(ByteOrder.nativeOrder())
        }
        
        // Initialize TFLite interpreter
        initializeInterpreter()
    }

    /**
     * Initialize the TFLite interpreter with GPU support if available.
     */
    private fun initializeInterpreter() {
        try {
            val model = loadModelFile()
            val options = Interpreter.Options()
            
            // Try to use GPU delegate if available
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                Log.d(TAG, "GPU delegate is supported, enabling...")
                gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                options.addDelegate(gpuDelegate)
            } else {
                Log.d(TAG, "GPU delegate not supported, using CPU")
                options.setNumThreads(4)
            }
            
            interpreter = Interpreter(model, options)
            Log.i(TAG, "TFLite interpreter initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize interpreter: ${e.message}", e)
        }
    }

    /**
     * Load the TFLite model from assets.
     */
    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILENAME)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Main analysis function called by CameraX for each frame.
     */
    override fun analyze(imageProxy: ImageProxy) {
        // Skip frames for performance
        frameCounter++
        if (frameCounter % FRAME_SKIP != 0) {
            imageProxy.close()
            return
        }
        
        // Skip if already processing
        if (isProcessing || interpreter == null) {
            imageProxy.close()
            return
        }
        
        isProcessing = true
        
        try {
            // Convert YUV to Bitmap
            val bitmap = imageProxy.toBitmap()
            
            if (bitmap != null) {
                // Crop to ROI (lower eyelid region)
                val roiBitmap = cropToROI(bitmap, imageProxy.imageInfo.rotationDegrees)
                
                // Run inference
                val confidence = runInference(roiBitmap)
                
                // Return result on main thread
                onResult(confidence, roiBitmap)
                
                // Clean up original bitmap (ROI bitmap passed to callback)
                if (bitmap != roiBitmap) {
                    bitmap.recycle()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing frame: ${e.message}", e)
            onResult(0f, null)
        } finally {
            isProcessing = false
            imageProxy.close()
        }
    }

    /**
     * Convert ImageProxy (YUV_420_888) to Bitmap.
     * 
     * CameraX provides images in YUV_420_888 format. This function converts
     * them to RGB Bitmap for model inference.
     */
    private fun ImageProxy.toBitmap(): Bitmap? {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y plane
        yBuffer.get(nv21, 0, ySize)
        
        // Copy VU planes (NV21 format: YYYYYYYYVUVU)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        // Convert NV21 to JPEG then to Bitmap
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, outputStream)
        val jpegBytes = outputStream.toByteArray()
        
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    /**
     * Crop the bitmap to the Region of Interest (lower eyelid area).
     * 
     * The ROI is defined as a rectangle relative to the image dimensions,
     * allowing flexible positioning based on where the user positions their eye.
     * 
     * @param bitmap Source bitmap from camera
     * @param rotationDegrees Rotation of the image from camera sensor
     * @return Cropped and resized bitmap for model input
     */
    private fun cropToROI(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        // First, rotate the bitmap if needed
        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
        
        // Calculate ROI bounds in pixels
        val left = (rotatedBitmap.width * roiLeft).toInt().coerceIn(0, rotatedBitmap.width - 1)
        val top = (rotatedBitmap.height * roiTop).toInt().coerceIn(0, rotatedBitmap.height - 1)
        val right = (rotatedBitmap.width * roiRight).toInt().coerceIn(left + 1, rotatedBitmap.width)
        val bottom = (rotatedBitmap.height * roiBottom).toInt().coerceIn(top + 1, rotatedBitmap.height)
        
        val roiWidth = right - left
        val roiHeight = bottom - top
        
        // Crop to ROI
        val croppedBitmap = Bitmap.createBitmap(
            rotatedBitmap,
            left,
            top,
            roiWidth,
            roiHeight
        )
        
        // Clean up rotated bitmap if it was created
        if (rotatedBitmap != bitmap) {
            rotatedBitmap.recycle()
        }
        
        // Resize to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(
            croppedBitmap,
            INPUT_SIZE,
            INPUT_SIZE,
            true
        )
        
        // Clean up cropped bitmap
        if (croppedBitmap != resizedBitmap) {
            croppedBitmap.recycle()
        }
        
        return resizedBitmap
    }

    /**
     * Run inference on the preprocessed bitmap.
     * 
     * @param bitmap Preprocessed 224x224 RGB bitmap
     * @return Confidence score: 0.0 = Non-Anemic, 1.0 = Anemic
     */
    private fun runInference(bitmap: Bitmap): Float {
        val interpreter = this.interpreter ?: return 0f
        
        // Prepare input buffer
        inputBuffer.rewind()
        
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        for (pixel in pixels) {
            // Extract RGB values
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            // Normalize to [-1, 1] for MobileNetV3
            inputBuffer.putFloat((r - MEAN) / STD)
            inputBuffer.putFloat((g - MEAN) / STD)
            inputBuffer.putFloat((b - MEAN) / STD)
        }
        
        // Run inference
        inputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
        
        // Return confidence score (clamped to [0, 1])
        return outputBuffer[0].coerceIn(0f, 1f)
    }

    /**
     * Update the ROI configuration dynamically.
     * 
     * @param left Left boundary as fraction of image width (0.0 - 1.0)
     * @param top Top boundary as fraction of image height (0.0 - 1.0)
     * @param right Right boundary as fraction of image width (0.0 - 1.0)
     * @param bottom Bottom boundary as fraction of image height (0.0 - 1.0)
     */
    fun updateROI(left: Float, top: Float, right: Float, bottom: Float) {
        roiLeft = left
        roiTop = top
        roiRight = right
        roiBottom = bottom
        Log.d(TAG, "ROI updated: ($roiLeft, $roiTop, $roiRight, $roiBottom)")
    }

    /**
     * Set the ROI to focus on the lower eyelid area.
     * This is the primary region for detecting pallor.
     */
    fun setLowerEyelidROI() {
        updateROI(0.15f, 0.55f, 0.85f, 0.85f)
    }

    /**
     * Set the ROI to focus on the entire eye area.
     */
    fun setFullEyeROI() {
        updateROI(0.1f, 0.2f, 0.9f, 0.9f)
    }

    /**
     * Clean up resources when no longer needed.
     * Call this when the analyzer is no longer in use.
     */
    fun close() {
        try {
            interpreter?.close()
            gpuDelegate?.close()
            interpreter = null
            gpuDelegate = null
            Log.d(TAG, "Analyzer resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing analyzer: ${e.message}", e)
        }
    }
}

/**
 * Extension function to get a human-readable risk level from confidence score.
 */
fun Float.toRiskLevel(): String = when {
    this < 0.3f -> "Low Risk (Normal)"
    this < 0.6f -> "Medium Risk (Borderline)"
    else -> "High Risk (Anemic)"
}

/**
 * Extension function to get a color resource for the risk level.
 * Returns a color int: Green for low, Yellow for medium, Red for high risk.
 */
fun Float.toRiskColor(): Int = when {
    this < 0.3f -> 0xFF4CAF50.toInt()   // Green
    this < 0.6f -> 0xFFFFC107.toInt()   // Yellow/Amber
    else -> 0xFFF44336.toInt()           // Red
}

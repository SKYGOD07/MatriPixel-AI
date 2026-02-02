package com.matripixel.ai.ml

import android.content.Context
import android.graphics.Bitmap
import com.matripixel.ai.data.model.ColorAnalysis
import com.matripixel.ai.data.model.DiagnosisResult
import com.matripixel.ai.data.model.Vitals
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * LiteRT-based Anemia Detection Engine
 * 
 * Uses a TensorFlow Lite model for offline inference.
 * Fallback to heuristic analysis if model is unavailable.
 * 
 * Model Input: 224x224 RGB image
 * Model Output: Risk score (0-1)
 */
class AnemiaDetector(private val context: Context) {
    
    companion object {
        private const val MODEL_FILE = "anemia_detector.tflite"
        private const val INPUT_SIZE = 224
        private const val PIXEL_SIZE = 3 // RGB
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
    }
    
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var isModelLoaded = false
    
    init {
        try {
            loadModel()
        } catch (e: Exception) {
            // Model not found - will use heuristic analysis
            android.util.Log.w("AnemiaDetector", "TFLite model not found, using heuristic analysis", e)
        }
    }
    
    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            
            // Try GPU acceleration first
            val options = Interpreter.Options()
            try {
                gpuDelegate = GpuDelegate()
                options.addDelegate(gpuDelegate)
            } catch (e: Exception) {
                android.util.Log.w("AnemiaDetector", "GPU delegate not available, using CPU", e)
            }
            
            interpreter = Interpreter(modelBuffer, options)
            isModelLoaded = true
            android.util.Log.i("AnemiaDetector", "TFLite model loaded successfully")
            
        } catch (e: Exception) {
            isModelLoaded = false
            throw e
        }
    }
    
    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Analyze an image for anemia indicators
     * 
     * @param bitmap Preprocessed 224x224 image
     * @param colorAnalysis Pre-computed color analysis
     * @param vitals Optional patient vitals
     * @return DiagnosisResult with risk score and recommendations
     */
    fun analyze(
        bitmap: Bitmap,
        colorAnalysis: ColorAnalysis,
        vitals: Vitals? = null
    ): DiagnosisResult {
        val startTime = System.currentTimeMillis()
        
        val (riskScore, confidence) = if (isModelLoaded) {
            runModelInference(bitmap)
        } else {
            // Fallback to heuristic analysis
            runHeuristicAnalysis(colorAnalysis)
        }
        
        // Adjust score based on vitals if available
        val adjustedScore = adjustScoreWithVitals(riskScore, vitals)
        
        val inferenceTime = System.currentTimeMillis() - startTime
        
        return DiagnosisResult.fromScore(
            score = adjustedScore,
            confidence = confidence,
            inferenceTimeMs = inferenceTime,
            colorAnalysis = colorAnalysis
        )
    }
    
    /**
     * Run TFLite model inference
     */
    private fun runModelInference(bitmap: Bitmap): Pair<Float, Float> {
        val interpreter = this.interpreter ?: return 0.5f to 0.5f
        
        // Preprocess image to model input format
        val inputBuffer = preprocessImage(bitmap)
        
        // Prepare output buffer
        val outputBuffer = Array(1) { FloatArray(2) } // [risk_score, confidence]
        
        // Run inference
        interpreter.run(inputBuffer, outputBuffer)
        
        val riskScore = outputBuffer[0][0].coerceIn(0f, 1f)
        val confidence = outputBuffer[0][1].coerceIn(0f, 1f)
        
        return riskScore to confidence
    }
    
    /**
     * Preprocess bitmap to ByteBuffer for model input
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaledBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8) and 0xFF)
            val b = (pixel and 0xFF)
            
            // Normalize to [-1, 1]
            byteBuffer.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
            byteBuffer.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
            byteBuffer.putFloat((b - IMAGE_MEAN) / IMAGE_STD)
        }
        
        return byteBuffer
    }
    
    /**
     * Heuristic analysis when model is not available
     * Based on color-based pallor detection research
     */
    private fun runHeuristicAnalysis(
        colorAnalysis: ColorAnalysis
    ): Pair<Float, Float> {
        // Base score from pallor index (higher pallor = higher risk)
        var riskScore = colorAnalysis.pallorIndex
        
        // Adjust based on specific color metrics
        // Low saturation indicates anemia
        if (colorAnalysis.saturation < 0.20f) {
            riskScore += 0.15f
        }
        
        // Low red channel relative to others indicates pallor
        val redRatio = colorAnalysis.meanRed / 
            (colorAnalysis.meanRed + colorAnalysis.meanGreen + colorAnalysis.meanBlue + 0.001f)
        if (redRatio < 0.35f) {
            riskScore += 0.10f
        }
        
        // High brightness with low saturation = pale
        if (colorAnalysis.brightness > 0.7f && colorAnalysis.saturation < 0.25f) {
            riskScore += 0.10f
        }
        
        // Clamp to valid range
        riskScore = riskScore.coerceIn(0f, 1f)
        
        // Confidence is lower for heuristic analysis
        val confidence = 0.65f
        
        return riskScore to confidence
    }
    
    /**
     * Adjust risk score based on patient vitals
     */
    private fun adjustScoreWithVitals(baseScore: Float, vitals: Vitals?): Float {
        if (vitals == null) return baseScore
        
        var adjustedScore = baseScore
        
        // Known low hemoglobin is a strong indicator
        vitals.knownHemoglobin?.let { hb ->
            when {
                hb < 7.0f -> adjustedScore += 0.3f   // Severe anemia
                hb < 10.0f -> adjustedScore += 0.15f // Moderate anemia
                hb < 12.0f -> adjustedScore += 0.05f // Mild anemia
            }
        }
        
        // High fatigue level
        vitals.fatigueLevel?.let { fatigue ->
            if (fatigue >= 7) adjustedScore += 0.10f
            else if (fatigue >= 5) adjustedScore += 0.05f
        }
        
        // Shortness of breath
        if (vitals.shortnessOfBreath) {
            adjustedScore += 0.08f
        }
        
        // Dizziness
        if (vitals.dizziness) {
            adjustedScore += 0.05f
        }
        
        // Pale skin reported
        if (vitals.paleSkin) {
            adjustedScore += 0.05f
        }
        
        return adjustedScore.coerceIn(0f, 1f)
    }
    
    /**
     * Release resources
     */
    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
        isModelLoaded = false
    }
    
    /**
     * Check if ML model is loaded
     */
    fun isModelAvailable(): Boolean = isModelLoaded
}

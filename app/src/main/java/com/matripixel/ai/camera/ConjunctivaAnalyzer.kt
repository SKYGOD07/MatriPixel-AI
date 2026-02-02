package com.matripixel.ai.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.matripixel.ai.data.model.ColorAnalysis
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * CameraX ImageAnalysis.Analyzer for conjunctiva/nail bed analysis.
 * 
 * Pipeline:
 * 1. Convert YUV to RGB bitmap
 * 2. Extract ROI (center region)
 * 3. Analyze color distribution
 * 4. Callback with preprocessed data
 */
class ConjunctivaAnalyzer(
    private val context: Context,
    private val onAnalysis: (Bitmap, ColorAnalysis) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer {
    
    companion object {
        // ROI is 40% of image centered
        private const val ROI_RATIO = 0.4f
        private const val TARGET_SIZE = 224 // Model input size
    }
    
    private var lastAnalysisTimestamp = 0L
    private val analysisIntervalMs = 500L // Analyze every 500ms
    
    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Throttle analysis to avoid overwhelming CPU
        if (currentTime - lastAnalysisTimestamp < analysisIntervalMs) {
            image.close()
            return
        }
        lastAnalysisTimestamp = currentTime
        
        try {
            // Convert ImageProxy to Bitmap
            val bitmap = image.toBitmap()
            
            // Extract ROI (center region where conjunctiva/nail should be)
            val roiBitmap = extractROI(bitmap)
            
            // Analyze color distribution
            val colorAnalysis = analyzeColors(roiBitmap)
            
            // Callback with results
            onAnalysis(roiBitmap, colorAnalysis)
            
        } catch (e: Exception) {
            onError(e)
        } finally {
            image.close()
        }
    }
    
    /**
     * Convert ImageProxy (YUV_420_888) to Bitmap
     */
    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        
        // Rotate based on image rotation
        val matrix = Matrix()
        matrix.postRotate(imageInfo.rotationDegrees.toFloat())
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Extract center ROI from bitmap
     */
    private fun extractROI(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val roiWidth = (width * ROI_RATIO).toInt()
        val roiHeight = (height * ROI_RATIO).toInt()
        
        val startX = (width - roiWidth) / 2
        val startY = (height - roiHeight) / 2
        
        // Extract center region
        val roiBitmap = Bitmap.createBitmap(bitmap, startX, startY, roiWidth, roiHeight)
        
        // Scale to model input size
        return Bitmap.createScaledBitmap(roiBitmap, TARGET_SIZE, TARGET_SIZE, true)
    }
    
    /**
     * Analyze color distribution in the image for pallor detection
     */
    private fun analyzeColors(bitmap: Bitmap): ColorAnalysis {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var totalSaturation = 0f
        var totalBrightness = 0f
        
        val hsv = FloatArray(3)
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            totalR += r
            totalG += g
            totalB += b
            
            // Convert to HSV for saturation/brightness
            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            totalSaturation += hsv[1]
            totalBrightness += hsv[2]
        }
        
        val pixelCount = pixels.size.toFloat()
        val meanR = totalR / pixelCount
        val meanG = totalG / pixelCount
        val meanB = totalB / pixelCount
        val meanSaturation = totalSaturation / pixelCount
        val meanBrightness = totalBrightness / pixelCount
        
        // Pallor Index: Lower red relative to overall brightness indicates pallor
        // Healthy conjunctiva: high red channel, high saturation
        // Anemic conjunctiva: low red, low saturation (pale/whitish)
        val pallorIndex = calculatePallorIndex(
            meanR.toFloat(), 
            meanG.toFloat(), 
            meanB.toFloat(),
            meanSaturation
        )
        
        return ColorAnalysis(
            meanRed = meanR.toFloat(),
            meanGreen = meanG.toFloat(),
            meanBlue = meanB.toFloat(),
            pallorIndex = pallorIndex,
            saturation = meanSaturation,
            brightness = meanBrightness
        )
    }
    
    /**
     * Calculate pallor index (0 = healthy, 1 = severe pallor)
     * Based on research: anemic conjunctiva shows reduced redness and saturation
     */
    private fun calculatePallorIndex(r: Float, g: Float, b: Float, saturation: Float): Float {
        // Normalized red ratio (healthy conjunctiva has high red dominance)
        val total = r + g + b + 0.001f // Avoid division by zero
        val redRatio = r / total
        
        // Expected healthy red ratio is around 0.45-0.55
        // Lower values indicate pallor
        val redDeficit = max(0f, 0.50f - redRatio) / 0.50f
        
        // Low saturation also indicates pallor (pale/whitish appearance)
        val saturationDeficit = max(0f, 0.30f - saturation) / 0.30f
        
        // Combined pallor index
        val pallorIndex = (redDeficit * 0.6f + saturationDeficit * 0.4f)
        
        return min(1f, max(0f, pallorIndex))
    }
}

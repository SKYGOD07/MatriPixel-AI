package com.matripixel.ai.ml

import android.graphics.Bitmap
import com.matripixel.ai.data.model.ColorAnalysis
import com.matripixel.ai.data.model.DiagnosisResult
import com.matripixel.ai.data.model.DiagnosisScan
import com.matripixel.ai.data.model.Patient
import com.matripixel.ai.data.model.ScanType
import com.matripixel.ai.data.model.Vitals
import com.matripixel.ai.data.repository.DiagnosisRepository
import com.matripixel.ai.data.repository.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the anemia detection workflow
 */
@Singleton
class DiagnosisUseCase @Inject constructor(
    private val anemiaDetector: AnemiaDetector,
    private val patientRepository: PatientRepository,
    private val diagnosisRepository: DiagnosisRepository
) {
    /**
     * Run full diagnosis workflow
     */
    suspend fun runDiagnosis(
        bitmap: Bitmap,
        colorAnalysis: ColorAnalysis,
        patient: Patient,
        vitals: Vitals,
        scanType: ScanType,
        localImagePath: String
    ): DiagnosisResult = withContext(Dispatchers.Default) {
        
        // Save patient if new
        patientRepository.insert(patient)
        
        // Run AI inference
        val result = anemiaDetector.analyze(bitmap, colorAnalysis, vitals)
        
        // Create feature vector for federated learning (no raw data)
        val featureVector = createFeatureVector(colorAnalysis, vitals)
        
        // Save diagnosis to database
        val scan = DiagnosisScan(
            patientId = patient.id,
            scanType = scanType,
            localImagePath = localImagePath,
            vitals = vitals,
            riskScore = result.riskScore,
            riskLevel = result.riskLevel,
            confidence = result.confidence,
            featureVector = featureVector,
            inferenceTimeMs = result.inferenceTimeMs
        )
        
        diagnosisRepository.insert(scan)
        
        result
    }
    
    /**
     * Create anonymized feature vector for federated learning
     * This can be safely synced to the cloud without privacy concerns
     */
    private fun createFeatureVector(
        colorAnalysis: ColorAnalysis,
        vitals: Vitals
    ): String {
        val json = JSONObject().apply {
            // Color features (anonymized)
            put("pallor_index", colorAnalysis.pallorIndex)
            put("saturation", colorAnalysis.saturation)
            put("brightness", colorAnalysis.brightness)
            put("red_ratio", colorAnalysis.meanRed / 
                (colorAnalysis.meanRed + colorAnalysis.meanGreen + colorAnalysis.meanBlue + 0.001f))
            
            // Vitals (no identifying info)
            put("has_fatigue", vitals.fatigueLevel != null && vitals.fatigueLevel >= 5)
            put("has_shortness_of_breath", vitals.shortnessOfBreath)
            put("has_dizziness", vitals.dizziness)
            put("has_pale_skin", vitals.paleSkin)
        }
        
        return json.toString()
    }
}

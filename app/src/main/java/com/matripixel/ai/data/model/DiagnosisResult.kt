package com.matripixel.ai.data.model

/**
 * Result of the anemia detection analysis.
 * Used as a domain model between ML layer and UI.
 */
data class DiagnosisResult(
    val riskScore: Float,       // 0.0 - 1.0
    val riskLevel: RiskLevel,
    val confidence: Float,
    val inferenceTimeMs: Long,
    val colorAnalysis: ColorAnalysis,
    val recommendation: String
) {
    companion object {
        fun fromScore(
            score: Float,
            confidence: Float,
            inferenceTimeMs: Long,
            colorAnalysis: ColorAnalysis
        ): DiagnosisResult {
            val (level, recommendation) = when {
                score >= 0.7f -> RiskLevel.RED to "Immediate medical consultation recommended. Signs suggest possible moderate to severe anemia."
                score >= 0.4f -> RiskLevel.AMBER to "Consider scheduling a blood test. Some indicators of mild anemia detected."
                else -> RiskLevel.GREEN to "No immediate concern detected. Continue regular health monitoring."
            }
            
            return DiagnosisResult(
                riskScore = score,
                riskLevel = level,
                confidence = confidence,
                inferenceTimeMs = inferenceTimeMs,
                colorAnalysis = colorAnalysis,
                recommendation = recommendation
            )
        }
    }
}

/**
 * Color analysis data from the conjunctiva/nail bed image
 */
data class ColorAnalysis(
    val meanRed: Float,
    val meanGreen: Float,
    val meanBlue: Float,
    val pallorIndex: Float,    // Calculated pallor metric
    val saturation: Float,
    val brightness: Float
)

package com.matripixel.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.matripixel.ai.ui.theme.*

/**
 * Risk Level enum for the gauge
 */
enum class RiskLevel(
    val color: Color,
    val label: String,
    val labelHindi: String
) {
    LOW(SafeGreen, "Low Risk", "कम जोखिम"),
    MODERATE(AmberWarning, "Moderate Risk", "मध्यम जोखिम"),
    HIGH(EmergencyRed, "High Risk", "उच्च जोखिम")
}

/**
 * RiskGauge - Animated arc gauge showing anemia risk level
 * Features:
 * - Gradient arc from Green → Amber → Red
 * - Animated needle indicator
 * - Large readable risk label
 * - High contrast for outdoor visibility
 */
@Composable
fun RiskGauge(
    modifier: Modifier = Modifier,
    riskScore: Float, // 0.0 (low) to 1.0 (high)
    size: Dp = 240.dp,
    strokeWidth: Dp = 24.dp,
    animated: Boolean = true
) {
    val riskLevel = when {
        riskScore < 0.4f -> RiskLevel.LOW
        riskScore < 0.7f -> RiskLevel.MODERATE
        else -> RiskLevel.HIGH
    }
    
    // Animate the needle
    val animatedScore by animateFloatAsState(
        targetValue = if (animated) riskScore else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "needle_animation"
    )
    
    // Pulse animation for high risk
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasSize = this.size
                val radius = (canvasSize.minDimension - strokeWidth.toPx()) / 2
                val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
                val arcSize = Size(radius * 2, radius * 2)
                val arcTopLeft = Offset(center.x - radius, center.y - radius)
                
                val startAngle = 135f // Start from bottom-left
                val sweepAngle = 270f // Sweep to bottom-right
                
                // Background arc (gray track)
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
                
                // Green segment (0-40%)
                drawArc(
                    color = SafeGreen,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * 0.4f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
                
                // Amber segment (40-70%)
                drawArc(
                    color = AmberWarning,
                    startAngle = startAngle + (sweepAngle * 0.4f),
                    sweepAngle = sweepAngle * 0.3f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
                
                // Red segment (70-100%)
                val redAlpha = if (riskLevel == RiskLevel.HIGH) pulseAlpha else 1f
                drawArc(
                    color = EmergencyRed.copy(alpha = redAlpha),
                    startAngle = startAngle + (sweepAngle * 0.7f),
                    sweepAngle = sweepAngle * 0.3f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
                
                // Needle indicator
                val needleAngle = startAngle + (sweepAngle * animatedScore)
                val needleLength = radius - 20.dp.toPx()
                val needleRadians = Math.toRadians(needleAngle.toDouble())
                val needleEnd = Offset(
                    x = center.x + (needleLength * kotlin.math.cos(needleRadians)).toFloat(),
                    y = center.y + (needleLength * kotlin.math.sin(needleRadians)).toFloat()
                )
                
                // Needle shadow
                drawLine(
                    color = Color.Black.copy(alpha = 0.3f),
                    start = center + Offset(2f, 2f),
                    end = needleEnd + Offset(2f, 2f),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Needle
                drawLine(
                    color = riskLevel.color,
                    start = center,
                    end = needleEnd,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Center circle
                drawCircle(
                    color = riskLevel.color,
                    radius = 16.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = SurfaceLight,
                    radius = 10.dp.toPx(),
                    center = center
                )
            }
            
            // Score percentage in center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = 40.dp)
            ) {
                Text(
                    text = "${(animatedScore * 100).toInt()}%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = riskLevel.color
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Risk Level Label
        Text(
            text = riskLevel.label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = riskLevel.color
        )
        Text(
            text = riskLevel.labelHindi,
            style = MaterialTheme.typography.titleLarge,
            color = riskLevel.color.copy(alpha = 0.8f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RiskGaugeLowPreview() {
    MatriPixelTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            RiskGauge(riskScore = 0.25f, animated = false)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RiskGaugeModeratePreview() {
    MatriPixelTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            RiskGauge(riskScore = 0.55f, animated = false)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RiskGaugeHighPreview() {
    MatriPixelTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            RiskGauge(riskScore = 0.88f, animated = false)
        }
    }
}

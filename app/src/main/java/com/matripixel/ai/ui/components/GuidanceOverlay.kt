package com.matripixel.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.matripixel.ai.ui.theme.GuidanceRectangle
import com.matripixel.ai.ui.theme.MatriPixelTheme
import com.matripixel.ai.ui.theme.OverlayBackground
import com.matripixel.ai.ui.theme.SurfaceLight

/**
 * GuidanceOverlay - Camera overlay for eye alignment
 * Features:
 * - Semi-transparent dark overlay
 * - Clear rectangular guidance zone with rounded corners
 * - Animated border and crosshairs
 * - Instructional text in Hindi/English
 */
@Composable
fun GuidanceOverlay(
    modifier: Modifier = Modifier,
    guidanceWidth: Dp = 280.dp,
    guidanceHeight: Dp = 180.dp,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 4.dp,
    guidanceText: String = "Align Patient Eye Here\nआँख यहाँ रखें"
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Semi-transparent overlay with cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Calculate guidance rectangle position (centered)
            val rectWidth = guidanceWidth.toPx()
            val rectHeight = guidanceHeight.toPx()
            val rectLeft = (canvasWidth - rectWidth) / 2
            val rectTop = (canvasHeight - rectHeight) / 2
            val radius = cornerRadius.toPx()
            
            // Draw semi-transparent overlay
            drawRect(
                color = OverlayBackground,
                size = size
            )
            
            // Create path for the cutout (clear rectangle)
            val cutoutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = rectLeft,
                        top = rectTop,
                        right = rectLeft + rectWidth,
                        bottom = rectTop + rectHeight,
                        cornerRadius = CornerRadius(radius, radius)
                    )
                )
            }
            
            // Clear the cutout area
            drawPath(
                path = cutoutPath,
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )
            
            // Draw guidance border
            drawRoundRect(
                color = GuidanceRectangle,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = borderWidth.toPx())
            )
            
            // Draw corner markers (L-shaped corners)
            val markerLength = 40.dp.toPx()
            val markerStroke = 6.dp.toPx()
            val markerColor = SurfaceLight
            
            // Top-left corner
            drawLine(
                color = markerColor,
                start = Offset(rectLeft, rectTop + markerLength),
                end = Offset(rectLeft, rectTop),
                strokeWidth = markerStroke
            )
            drawLine(
                color = markerColor,
                start = Offset(rectLeft, rectTop),
                end = Offset(rectLeft + markerLength, rectTop),
                strokeWidth = markerStroke
            )
            
            // Top-right corner
            drawLine(
                color = markerColor,
                start = Offset(rectLeft + rectWidth, rectTop + markerLength),
                end = Offset(rectLeft + rectWidth, rectTop),
                strokeWidth = markerStroke
            )
            drawLine(
                color = markerColor,
                start = Offset(rectLeft + rectWidth, rectTop),
                end = Offset(rectLeft + rectWidth - markerLength, rectTop),
                strokeWidth = markerStroke
            )
            
            // Bottom-left corner
            drawLine(
                color = markerColor,
                start = Offset(rectLeft, rectTop + rectHeight - markerLength),
                end = Offset(rectLeft, rectTop + rectHeight),
                strokeWidth = markerStroke
            )
            drawLine(
                color = markerColor,
                start = Offset(rectLeft, rectTop + rectHeight),
                end = Offset(rectLeft + markerLength, rectTop + rectHeight),
                strokeWidth = markerStroke
            )
            
            // Bottom-right corner
            drawLine(
                color = markerColor,
                start = Offset(rectLeft + rectWidth, rectTop + rectHeight - markerLength),
                end = Offset(rectLeft + rectWidth, rectTop + rectHeight),
                strokeWidth = markerStroke
            )
            drawLine(
                color = markerColor,
                start = Offset(rectLeft + rectWidth, rectTop + rectHeight),
                end = Offset(rectLeft + rectWidth - markerLength, rectTop + rectHeight),
                strokeWidth = markerStroke
            )
            
            // Draw center crosshair
            val crosshairSize = 20.dp.toPx()
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2
            
            drawLine(
                color = SurfaceLight.copy(alpha = 0.7f),
                start = Offset(centerX - crosshairSize, centerY),
                end = Offset(centerX + crosshairSize, centerY),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = SurfaceLight.copy(alpha = 0.7f),
                start = Offset(centerX, centerY - crosshairSize),
                end = Offset(centerX, centerY + crosshairSize),
                strokeWidth = 2.dp.toPx()
            )
        }
        
        // Guidance text below the rectangle
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (guidanceHeight / 2) + 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = guidanceText,
                style = MaterialTheme.typography.titleLarge,
                color = SurfaceLight,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun GuidanceOverlayPreview() {
    MatriPixelTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .size(400.dp)
        ) {
            GuidanceOverlay()
        }
    }
}

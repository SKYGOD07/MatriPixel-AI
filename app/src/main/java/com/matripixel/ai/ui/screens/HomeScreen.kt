package com.matripixel.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.matripixel.ai.ui.theme.*

/**
 * HomeScreen - Dashboard for ASHA Workers
 * Features: New Screening, Pending Syncs, High Risk Alerts
 * Design: Large buttons, high contrast for low-literacy users
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    pendingSyncs: Int = 0,
    highRiskAlerts: List<HighRiskAlert> = emptyList(),
    onNewScreeningClick: () -> Unit = {},
    onPendingSyncsClick: () -> Unit = {},
    onAlertClick: (HighRiskAlert) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MatriPixel",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // === NEW SCREENING - Primary Action ===
            NewScreeningCard(onClick = onNewScreeningClick)
            
            // === STATUS ROW ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pending Syncs Card
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending\nSyncs",
                    count = pendingSyncs,
                    icon = Icons.Filled.CloudUpload,
                    containerColor = if (pendingSyncs > 0) 
                        MaterialTheme.colorScheme.tertiaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (pendingSyncs > 0) 
                        MaterialTheme.colorScheme.onTertiaryContainer 
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onPendingSyncsClick
                )
                
                // Total Screenings Today
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Today's\nScreenings",
                    count = 12, // TODO: Get from ViewModel
                    icon = Icons.Filled.Assessment,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { }
                )
            }
            
            // === HIGH RISK ALERTS ===
            if (highRiskAlerts.isNotEmpty()) {
                HighRiskAlertsSection(
                    alerts = highRiskAlerts,
                    onAlertClick = onAlertClick
                )
            } else {
                NoAlertsCard()
            }
            
            // Spacer for bottom padding
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Large primary action button for starting new screening
 */
@Composable
fun NewScreeningCard(
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MedicalBlue,
                            MedicalBlueLight
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = SurfaceLight
                )
                Column {
                    Text(
                        text = "New Screening",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceLight
                    )
                    Text(
                        text = "Tap to scan patient's eye",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SurfaceLight.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

/**
 * Compact status card showing count with icon
 */
@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * High Risk Alerts Section with emergency styling
 */
@Composable
fun HighRiskAlertsSection(
    alerts: List<HighRiskAlert>,
    onAlertClick: (HighRiskAlert) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = EmergencyRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "High Risk Alerts",
                style = MaterialTheme.typography.titleLarge,
                color = EmergencyRed,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmergencyRedContainer
            ) {
                Text(
                    text = "${alerts.size}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnEmergencyRedContainer
                )
            }
        }
        
        // Alert Cards
        alerts.forEach { alert ->
            HighRiskAlertCard(alert = alert, onClick = { onAlertClick(alert) })
        }
    }
}

@Composable
fun HighRiskAlertCard(
    alert: HighRiskAlert,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = EmergencyRedContainer
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, EmergencyRed)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.patientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnEmergencyRedContainer
                )
                Text(
                    text = alert.timeAgo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnEmergencyRedContainer.copy(alpha = 0.7f)
                )
            }
            FilledTonalButton(
                onClick = onClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = EmergencyRed,
                    contentColor = SurfaceLight
                )
            ) {
                Text("View", style = MaterialTheme.typography.labelLarge)
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun NoAlertsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SafeGreenContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SafeGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "No High Risk Alerts",
                style = MaterialTheme.typography.titleMedium,
                color = OnSafeGreenContainer
            )
        }
    }
}

// === Data Classes ===
data class HighRiskAlert(
    val id: String,
    val patientName: String,
    val timeAgo: String,
    val riskScore: Float
)

// === Preview ===
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MatriPixelTheme {
        HomeScreen(
            pendingSyncs = 3,
            highRiskAlerts = listOf(
                HighRiskAlert("1", "Priya Devi", "2 hours ago", 0.92f),
                HighRiskAlert("2", "Sunita Kumari", "5 hours ago", 0.85f)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenNoAlertsPreview() {
    MatriPixelTheme {
        HomeScreen(
            pendingSyncs = 0,
            highRiskAlerts = emptyList()
        )
    }
}

package com.matripixel.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.matripixel.ai.ui.components.RiskGauge
import com.matripixel.ai.ui.components.RiskLevel
import com.matripixel.ai.ui.theme.*

/**
 * ResultScreen - Displays screening result with risk gauge
 * Features:
 * - Animated Risk Gauge (Red/Amber/Green)
 * - Conditional actions based on risk level
 * - Large, accessible buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    riskScore: Float, // 0.0 (low) to 1.0 (high)
    patientName: String = "",
    onGenerateReferral: () -> Unit = {},
    onNewScreening: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    val riskLevel = when {
        riskScore < 0.4f -> RiskLevel.LOW
        riskScore < 0.7f -> RiskLevel.MODERATE
        else -> RiskLevel.HIGH
    }
    
    val backgroundColor = when (riskLevel) {
        RiskLevel.HIGH -> EmergencyRedContainer
        RiskLevel.MODERATE -> AmberContainer
        RiskLevel.LOW -> SafeGreenContainer
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Screening Result",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = riskLevel.color,
                    titleContentColor = SurfaceLight,
                    navigationIconContentColor = SurfaceLight
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(backgroundColor)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Patient Name Card (if provided)
            if (patientName.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MedicalBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Patient",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            Text(
                                text = patientName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Risk Gauge Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Anemia Risk Assessment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "रक्ताल्पता जोखिम आकलन",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMuted
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // The Risk Gauge
                    RiskGauge(
                        riskScore = riskScore,
                        size = 260.dp,
                        strokeWidth = 28.dp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Risk explanation
                    Text(
                        text = when (riskLevel) {
                            RiskLevel.HIGH -> "Immediate medical attention recommended"
                            RiskLevel.MODERATE -> "Follow-up screening recommended"
                            RiskLevel.LOW -> "No immediate concerns detected"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = riskLevel.color
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // === CONDITIONAL ACTION BUTTONS ===
            
            AnimatedVisibility(
                visible = riskLevel == RiskLevel.HIGH,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                // HIGH RISK: Generate Referral Ticket
                Button(
                    onClick = onGenerateReferral,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Generate Referral Ticket",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "रेफरल टिकट बनाएं",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = riskLevel == RiskLevel.MODERATE,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                // MODERATE RISK: Retest Recommended
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AmberWarning.copy(alpha = 0.2f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, AmberWarning)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = AmberWarningDark,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Retest Recommended",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarningDark
                            )
                            Text(
                                text = "दोबारा जाँच की सलाह",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmberWarningDark.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            AnimatedVisibility(
                visible = riskLevel == RiskLevel.LOW,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                // LOW RISK: All Clear
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SafeGreen.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, SafeGreen)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Low Risk",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreenDark
                            )
                            Text(
                                text = "कम जोखिम - चिंता की कोई बात नहीं",
                                style = MaterialTheme.typography.bodyLarge,
                                color = SafeGreenDark.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Secondary Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // New Screening Button
                OutlinedButton(
                    onClick = onNewScreening,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MedicalBlue
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MedicalBlue)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Scan",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                
                // View History Button
                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// === Previews ===

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ResultScreenHighRiskPreview() {
    MatriPixelTheme {
        ResultScreen(
            riskScore = 0.88f,
            patientName = "Priya Devi"
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ResultScreenModeratePreview() {
    MatriPixelTheme {
        ResultScreen(
            riskScore = 0.55f,
            patientName = "Sunita Kumari"
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ResultScreenLowRiskPreview() {
    MatriPixelTheme {
        ResultScreen(
            riskScore = 0.22f,
            patientName = "Rekha Singh"
        )
    }
}

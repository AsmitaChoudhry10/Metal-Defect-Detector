package org.bharatai.ps5.metalinspection

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bharatai.ps5.metalinspection.ui.theme.CoolBlue
import org.bharatai.ps5.metalinspection.ui.theme.ElectricCyan
import org.bharatai.ps5.metalinspection.ui.theme.GraphiteSlate
import org.bharatai.ps5.metalinspection.ui.theme.HazardAmber
import org.bharatai.ps5.metalinspection.ui.theme.LaserCrimson
import org.bharatai.ps5.metalinspection.ui.theme.LowSeverity
import org.bharatai.ps5.metalinspection.ui.theme.MetallicShineEnd
import org.bharatai.ps5.metalinspection.ui.theme.MetallicShineMid
import org.bharatai.ps5.metalinspection.ui.theme.MetallicShineStart
import org.bharatai.ps5.metalinspection.ui.theme.NeonEmerald
import org.bharatai.ps5.metalinspection.ui.theme.PrecisionBorder
import org.bharatai.ps5.metalinspection.ui.theme.PrecisionBorderBright
import org.bharatai.ps5.metalinspection.ui.theme.SlateSurface
import org.bharatai.ps5.metalinspection.ui.theme.SlateSurfaceElevated
import org.bharatai.ps5.metalinspection.ui.theme.SlateSurfaceVariant
import org.bharatai.ps5.metalinspection.ui.theme.SteelRecessed
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    Welcome,
    Inspect,
    Tutorial,
    About,
    History,
}

internal fun metallicBorderStroke(bright: Boolean = false): BorderStroke = BorderStroke(
    width = 1.dp,
    brush = Brush.verticalGradient(
        colors = if (bright) {
            listOf(PrecisionBorderBright, PrecisionBorder, Color(0xFF282E38))
        } else {
            listOf(Color(0xFF6B7787), PrecisionBorder, Color(0xFF232830))
        },
    ),
)

internal fun Modifier.brushedMetalFinish(elevated: Boolean = false): Modifier = this.drawBehind {
    val baseColors = if (elevated) {
        listOf(MetallicShineStart, MetallicShineMid, MetallicShineEnd)
    } else {
        listOf(Color(0xFF353C47), SlateSurface, Color(0xFF242931))
    }
    drawRect(
        brush = Brush.linearGradient(
            colors = baseColors,
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.35f, size.height),
        )
    )
    // Subtle horizontal brushed-steel grain
    val stepPx = 4.dp.toPx()
    var y = stepPx
    var idx = 0
    while (y < size.height) {
        val lineColor = if (idx % 2 == 0) {
            Color.White.copy(alpha = 0.025f)
        } else {
            Color.Black.copy(alpha = 0.03f)
        }
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f,
        )
        y += stepPx
        idx++
    }
    // Crisp specular top rim highlight
    drawLine(
        color = Color.White.copy(alpha = if (elevated) 0.16f else 0.10f),
        start = Offset(0f, 1f),
        end = Offset(size.width, 1f),
        strokeWidth = 1.5f,
    )
}

// ---------------------------------------------------------------------------
// 1. WELCOME FLOW: Crisp Brushed-Metal Opening Screen
// ---------------------------------------------------------------------------

@Composable
fun WelcomeScreen(
    onStartInspection: () -> Unit,
    onViewTutorial: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Metallic Identity Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = metallicBorderStroke(bright = true),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .brushedMetalFinish(elevated = true)
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Surface(
                                modifier = Modifier.size(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = SlateSurfaceVariant,
                                border = BorderStroke(1.5.dp, CoolBlue.copy(alpha = 0.5f)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    AppGlyph(AppGlyphKind.Scan, Modifier.size(28.dp), CoolBlue)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "Metal Inspection Assistant",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Text(
                                    text = "On-Device Metal Surface Inspection & Defect Identification",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SteelRecessed,
                            border = BorderStroke(1.dp, PrecisionBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = NeonEmerald) {}
                                Text(
                                    "Ready for inspection · 100% on-device edge processing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // Core Capabilities Grid
            Text(
                "Inspection Workflow",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )

            CapabilityCard(
                glyph = AppGlyphKind.Camera,
                tint = CoolBlue,
                title = "Optical Surface Acquisition",
                description = "Capture live specimen photos or import high-resolution images of steel strips, welds, and machined parts.",
            )

            CapabilityCard(
                glyph = AppGlyphKind.Metrology,
                tint = ElectricCyan,
                title = "Deterministic Surface Metrology",
                description = "Measures defect area density percentage and counts micro-contours in real time using OpenCV edge morphology.",
            )

            CapabilityCard(
                glyph = AppGlyphKind.Shield,
                tint = NeonEmerald,
                title = "Decision Support & Audit Ledger",
                description = "Provides step-by-step shop-floor SOP recommendations and logs every inspection locally in an on-device SQLite ledger.",
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Call to Action
            Button(
                onClick = onStartInspection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoolBlue,
                    contentColor = Color(0xFF0B1320),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
            ) {
                AppGlyph(AppGlyphKind.Scan, Modifier.size(20.dp), Color(0xFF0B1320))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Start Inspection",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }

            // Secondary Tutorial Action
            OutlinedButton(
                onClick = onViewTutorial,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PrecisionBorderBright),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                AppGlyph(AppGlyphKind.Book, Modifier.size(18.dp), ElectricCyan)
                Spacer(Modifier.width(8.dp))
                Text("View Tutorial", style = MaterialTheme.typography.labelLarge)
            }

            // Secondary Quick Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SlateSurfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = BorderStroke(1.dp, PrecisionBorder),
                ) {
                    AppGlyph(AppGlyphKind.History, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("History", style = MaterialTheme.typography.labelMedium)
                }

                FilledTonalButton(
                    onClick = onOpenAbout,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SlateSurfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = BorderStroke(1.dp, PrecisionBorder),
                ) {
                    AppGlyph(AppGlyphKind.Info, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("About & Info", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Trust & Offline Footer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = SteelRecessed,
                border = BorderStroke(1.dp, PrecisionBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppGlyph(AppGlyphKind.Shield, Modifier.size(16.dp), ElectricCyan)
                    Text(
                        "Air-gapped edge operation. No cloud account or internet connection required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityCard(
    glyph: AppGlyphKind,
    tint: Color,
    title: String,
    description: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = metallicBorderStroke(bright = false),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .brushedMetalFinish(elevated = false)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = SlateSurfaceVariant,
                border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AppGlyph(glyph, Modifier.size(20.dp), tint)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. FIRST-USE TUTORIAL SCREEN: Step-by-Step Operator Guide
// ---------------------------------------------------------------------------

private data class TutorialStep(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val body: String,
    val practicalTip: String,
    val glyph: AppGlyphKind,
    val accent: Color,
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        stepNumber = 1,
        title = "Capture or Select Specimen",
        subtitle = "Optical Specimen Input",
        body = "Use your device camera to photograph the steel component or surface area of interest, or tap 'Import' to select a pre-captured high-resolution image from your device storage.",
        practicalTip = "Tip: Clean oil films and loose metal shavings from the surface before photographing for the most accurate inspection.",
        glyph = AppGlyphKind.Camera,
        accent = CoolBlue,
    ),
    TutorialStep(
        stepNumber = 2,
        title = "Position and Balance Lighting",
        subtitle = "Even Illumination Best Practice",
        body = "Center the target surface within the frame. Ensure uniform, diffused illumination across the inspection area. Avoid intense specular highlights, deep shadows, or hand motion blur.",
        practicalTip = "Tip: If the preview shows heavy glare or out-of-focus areas, tap 'Capture' again to retake before analyzing.",
        glyph = AppGlyphKind.Scan,
        accent = ElectricCyan,
    ),
    TutorialStep(
        stepNumber = 3,
        title = "Run On-Device Analysis",
        subtitle = "Offline Edge Pipeline Execution",
        body = "Tap 'ANALYZE SPECIMEN' to process the image. The vision pipeline runs entirely on your device CPU via OpenCV, executing local contrast normalization, HOG feature extraction, and linear classification.",
        practicalTip = "Tip: Execution takes only ~40–50 ms. No data leaves your phone, and zero internet connection is required.",
        glyph = AppGlyphKind.Cpu,
        accent = CoolBlue,
    ),
    TutorialStep(
        stepNumber = 4,
        title = "Review Metrology & Severity",
        subtitle = "Automated Density & Defect Classification",
        body = "Inspect the predicted pattern (e.g., Scratches, Crazing, Inclusions, or Nominal baseline), defect density percentage, micro-contour count, and the standard operating procedure (SOP) recommendations.",
        practicalTip = "Tip: Defect density reflects the percentage of surface pixels showing anomalous gradient variance (<1% Nominal, 1-2.5% Low, 2.5-8% Medium, >=8% High).",
        glyph = AppGlyphKind.Metrology,
        accent = HazardAmber,
    ),
    TutorialStep(
        stepNumber = 5,
        title = "Decision Support & QA Process",
        subtitle = "Engineering Responsibility Notice",
        body = "Treat the results as decision support. The app does not measure physical defect depth and cannot provide certified structural or metallurgical acceptance. Always follow official facility inspection protocols.",
        practicalTip = "Tip: Inspectors can record visual overrides ('Pass / No Defect' or severity grading) directly in the local audit ledger.",
        glyph = AppGlyphKind.Shield,
        accent = NeonEmerald,
    ),
)

@Composable
fun TutorialScreen(
    onDone: () -> Unit,
    onStartInspection: () -> Unit,
) {
    BackHandler { onDone() }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = TUTORIAL_STEPS[currentStepIndex]

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSurfaceVariant,
                        border = BorderStroke(1.dp, PrecisionBorder),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onDone() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AppGlyph(AppGlyphKind.Back, Modifier.size(16.dp), Color.White)
                        }
                    }
                    Column {
                        Text("Inspection Tutorial", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text("Step ${currentStepIndex + 1} of ${TUTORIAL_STEPS.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                TextButton(onClick = onDone) {
                    Text("Skip", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Step Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TUTORIAL_STEPS.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                color = if (idx <= currentStepIndex) step.accent else SlateSurfaceVariant,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            // Step Content Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = metallicBorderStroke(bright = true),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .brushedMetalFinish(elevated = true)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = SlateSurfaceVariant,
                                border = BorderStroke(1.5.dp, step.accent.copy(alpha = 0.5f)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    AppGlyph(step.glyph, Modifier.size(24.dp), step.accent)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("STEP ${step.stepNumber}", style = MaterialTheme.typography.labelSmall, color = step.accent)
                                Text(step.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }

                        HorizontalDivider(color = PrecisionBorder)

                        Text(
                            text = step.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 24.sp,
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = SteelRecessed,
                            border = BorderStroke(1.dp, PrecisionBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                AppGlyph(AppGlyphKind.Check, Modifier.size(16.dp), step.accent)
                                Text(
                                    text = step.practicalTip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (currentStepIndex > 0) {
                    OutlinedButton(
                        onClick = { currentStepIndex-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PrecisionBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) {
                        Text("Previous", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (currentStepIndex < TUTORIAL_STEPS.lastIndex) {
                    Button(
                        onClick = { currentStepIndex++ },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoolBlue,
                            contentColor = Color(0xFF0B1320),
                        ),
                    ) {
                        Text("Next", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.width(6.dp))
                        AppGlyph(AppGlyphKind.ArrowRight, Modifier.size(16.dp), Color(0xFF0B1320))
                    }
                } else {
                    Button(
                        onClick = onStartInspection,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonEmerald,
                            contentColor = Color(0xFF062319),
                        ),
                    ) {
                        AppGlyph(AppGlyphKind.Scan, Modifier.size(18.dp), Color(0xFF062319))
                        Spacer(Modifier.width(8.dp))
                        Text("Start Inspection", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. ABOUT & TECHNICAL INFORMATION SCREEN
// ---------------------------------------------------------------------------

@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, PrecisionBorder),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onBack() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AppGlyph(AppGlyphKind.Back, Modifier.size(16.dp), Color.White)
                    }
                }
                Column {
                    Text("About This App", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text("Technical specifications & verification details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            AboutSectionCard(
                title = "Application Purpose & Identity",
                glyph = AppGlyphKind.Scan,
                accent = CoolBlue,
            ) {
                Text(
                    "Metal Inspection Assistant provides deterministic, on-device optical surface inspection for manufactured metal parts, steel coils, and fabricated components. All image analysis and data storage run completely offline on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                )
            }

            AboutSectionCard(
                title = "Models & Datasets",
                glyph = AppGlyphKind.Cpu,
                accent = ElectricCyan,
            ) {
                Text(
                    "• Primary Surface Classifier: Linear Support Vector Machine (SVM) paired with Histogram of Oriented Gradients (HOG) feature descriptors (200x200 ROI window, 40x40 pixel blocks, 20x20 pixel cells, 9 orientation bins, yielding a 4,410-dimensional feature vector).\n\n" +
                    "• Training & Validation Dataset: NEU-DET (Northeastern University Surface Defect Database), comprising 1,800 hot-rolled steel strip images across 6 defect classes: crazing, inclusion, patches, pitted surface, rolled-in scale, and scratches.\n\n" +
                    "• Advisory Reasoning Model: ExecuTorch + Llama 3.2 1B Instruct running locally on the device CPU via XNNPACK backend for structured SOP advice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                )
            }

            AboutSectionCard(
                title = "Evaluation Metrics & Test Set",
                glyph = AppGlyphKind.Check,
                accent = NeonEmerald,
            ) {
                Text(
                    "• Held-Out Evaluation Set: 270 NEU-DET images (45 images strictly isolated per class, never seen during model training).\n\n" +
                    "• Classification Accuracy: 83.70% overall test accuracy.\n\n" +
                    "• Macro F1 Score: 0.837 macro-averaged F1.\n\n" +
                    "• Execution Latency: Dynamically measured per-specimen on the device CPU at runtime (typically ~40–50 ms total pipeline latency on ARM Cortex-A). The app does not claim fixed inference times or unverified NPU hardware acceleration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                )
            }

            AboutSectionCard(
                title = "Severity Calculation & Metrology",
                glyph = AppGlyphKind.Metrology,
                accent = HazardAmber,
            ) {
                Text(
                    "Surface severity is computed deterministically from pixel gradients using OpenCV edge morphology (5x5 Gaussian smoothing followed by 21x21 adaptive Gaussian thresholding):\n\n" +
                    "• Defect Area Density (δ): Percentage of 40,000 pixels in the 200x200 ROI that exceed local surface contrast variance.\n\n" +
                    "• Calibrated Severity Thresholds:\n" +
                    "   – Nominal: δ < 1.0% (within baseline surface variation; no flaws detected)\n" +
                    "   – Low Severity: 1.0% ≤ δ < 2.5% (isolated superficial marks)\n" +
                    "   – Medium Severity: 2.5% ≤ δ < 8.0% (localized groove clusters)\n" +
                    "   – High Severity: δ ≥ 8.0% (extensive surface damage or micro-fissures)\n\n" +
                    "• Micro-contour count reflects distinct connected anomaly regions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                )
            }

            AboutSectionCard(
                title = "Experimental & Synthetic Features",
                glyph = AppGlyphKind.Info,
                accent = CoolBlue,
            ) {
                Text(
                    "• KolektorSDD2 Binary Detector: An experimental MobileNet ONNX binary classifier trained on electrical commutator defect images. It is domain-specific and unvalidated for general steel inspection.\n\n" +
                    "• Synthetic Data Models: Research models evaluated on synthetic imagery are strictly excluded from the active competition inspection flow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                )
            }

            AboutSectionCard(
                title = "Privacy, Storage & Air-Gapped Operation",
                glyph = AppGlyphKind.Shield,
                accent = NeonEmerald,
            ) {
                Text(
                    "• 100% On-Device Processing: No images, camera feeds, or inspection records are ever uploaded to cloud servers or remote endpoints.\n\n" +
                    "• Local Ledger Storage: Inspection history and human audit overrides are stored in an on-device SQLite database (inspection_history.db).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                )
            }

            AboutSectionCard(
                title = "Essential Disclaimers & Operational Limits",
                glyph = AppGlyphKind.Shield,
                accent = LaserCrimson,
            ) {
                Text(
                    "• Decision Support Only: This application assists human visual inspection. It is not an ISO, ASTM, or metallurgical certified standard and cannot guarantee acceptance or rejection of parts.\n\n" +
                    "• Optical Surface Limitations: 2D optical analysis cannot measure physical defect depth, subsurface fatigue, or structural micro-cracks.\n\n" +
                    "• Confidence vs. Severity: Model classification confidence is a measure of pattern certainty, not structural risk. Official engineering decisions must follow certified physical testing procedures.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AboutSectionCard(
    title: String,
    glyph: AppGlyphKind,
    accent: Color,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = metallicBorderStroke(bright = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .brushedMetalFinish(elevated = false)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AppGlyph(glyph, Modifier.size(16.dp), accent)
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            HorizontalDivider(color = PrecisionBorder)
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// 4. INSPECTION HISTORY LEDGER SCREEN
// ---------------------------------------------------------------------------

@Composable
internal fun HistoryScreen(
    records: List<InspectionRecord>,
    onBack: () -> Unit,
    onStartNewInspection: () -> Unit,
) {
    BackHandler { onBack() }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, PrecisionBorder),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onBack() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AppGlyph(AppGlyphKind.Back, Modifier.size(16.dp), Color.White)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Inspection Ledger", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text("${records.size} records preserved locally in SQLite", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(
                    onClick = onStartNewInspection,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CoolBlue.copy(alpha = 0.15f),
                        contentColor = CoolBlue,
                    ),
                    border = BorderStroke(1.dp, CoolBlue.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("New", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = SlateSurfaceVariant,
                            border = BorderStroke(1.dp, PrecisionBorder),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AppGlyph(AppGlyphKind.History, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("No inspection records yet", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            "Acquired specimens and automated findings will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onStartNewInspection,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoolBlue, contentColor = Color(0xFF0B1320)),
                        ) {
                            Text("Start First Inspection", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(records) { record ->
                        val isNominal = record.label.contains("clean", ignoreCase = true) ||
                                record.label.contains("pass", ignoreCase = true) ||
                                record.severity.contains("Pass", ignoreCase = true) ||
                                record.severity.contains("Nominal", ignoreCase = true)
                        val isHigh = record.severity.contains("High", ignoreCase = true)
                        val isMed = record.severity.contains("Medium", ignoreCase = true)

                        val statusColor = when {
                            isNominal -> NeonEmerald
                            isHigh -> LaserCrimson
                            isMed -> HazardAmber
                            else -> LowSeverity
                        }

                        val statusLabel = when {
                            isNominal -> "NOMINAL"
                            isHigh -> "HIGH"
                            isMed -> "MEDIUM"
                            else -> "LOW"
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Transparent,
                            border = metallicBorderStroke(bright = false),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .brushedMetalFinish(elevated = false)
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = if (isNominal) "Nominal Surface" else "${record.label.replace('_', ' ').replace('-', ' ').uppercase()} DEFECT",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isNominal) NeonEmerald else Color.White,
                                    )
                                    Text(
                                        text = "${dateFormat.format(Date(record.inspectedAt))} · ${record.durationMs} ms edge latency",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Severity: ${record.severity}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8),
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = statusColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                                ) {
                                    Text(
                                        text = statusLabel,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = statusColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. VECTOR GLYPHS: Lightweight Custom Canvas Icons
// ---------------------------------------------------------------------------

enum class AppGlyphKind {
    Scan,
    Camera,
    Gallery,
    Check,
    Metrology,
    Cpu,
    Shield,
    Book,
    Info,
    History,
    Back,
    ArrowRight,
}

@Composable
fun AppGlyph(kind: AppGlyphKind, modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val side = size.minDimension
        val inset = side * 0.12f
        val right = size.width - inset
        val bottom = size.height - inset
        val stroke = Stroke(width = side * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            AppGlyphKind.Scan -> {
                val corner = side * 0.28f
                drawLine(tint, Offset(inset, inset + corner), Offset(inset, inset), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(inset, inset), Offset(inset + corner, inset), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(right - corner, inset), Offset(right, inset), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(right, inset), Offset(right, inset + corner), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(inset, bottom - corner), Offset(inset, bottom), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(inset, bottom), Offset(inset + corner, bottom), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(right - corner, bottom), Offset(right, bottom), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(right, bottom), Offset(right, bottom - corner), stroke.width, StrokeCap.Round)
                drawCircle(tint, radius = side * 0.10f, center = Offset(size.width / 2, size.height / 2), style = stroke)
            }
            AppGlyphKind.Camera -> {
                val body = Path().apply {
                    moveTo(inset, side * 0.34f); lineTo(side * 0.36f, side * 0.34f)
                    lineTo(side * 0.45f, side * 0.22f); lineTo(side * 0.67f, side * 0.22f)
                    lineTo(side * 0.76f, side * 0.34f); lineTo(right, side * 0.34f)
                    lineTo(right, bottom); lineTo(inset, bottom); close()
                }
                drawPath(body, tint, style = stroke)
                drawCircle(tint, radius = side * 0.18f, center = Offset(size.width / 2, size.height * 0.63f), style = stroke)
            }
            AppGlyphKind.Gallery -> {
                drawRoundRect(tint, Offset(inset, inset), Size(side * 0.72f, side * 0.72f), androidx.compose.ui.geometry.CornerRadius(side * 0.08f), style = stroke)
                drawCircle(tint, radius = side * 0.065f, center = Offset(side * 0.34f, side * 0.34f))
                val hills = Path().apply {
                    moveTo(side * 0.18f, side * 0.78f); lineTo(side * 0.43f, side * 0.52f)
                    lineTo(side * 0.57f, side * 0.66f); lineTo(side * 0.68f, side * 0.55f)
                    lineTo(side * 0.86f, side * 0.74f)
                }
                drawPath(hills, tint, style = stroke)
            }
            AppGlyphKind.Check -> {
                drawCircle(tint, radius = side * 0.39f, center = Offset(size.width / 2, size.height / 2), style = stroke)
                val check = Path().apply {
                    moveTo(side * 0.28f, side * 0.51f); lineTo(side * 0.44f, side * 0.66f); lineTo(side * 0.73f, side * 0.35f)
                }
                drawPath(check, tint, style = stroke)
            }
            AppGlyphKind.Metrology -> {
                drawLine(tint, Offset(inset, side * 0.3f), Offset(right, side * 0.3f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(inset, side * 0.7f), Offset(right, side * 0.7f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(side * 0.3f, side * 0.3f), Offset(side * 0.3f, side * 0.7f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(side * 0.7f, side * 0.3f), Offset(side * 0.7f, side * 0.7f), stroke.width, StrokeCap.Round)
            }
            AppGlyphKind.Cpu -> {
                drawRoundRect(tint, Offset(side * 0.25f, side * 0.25f), Size(side * 0.5f, side * 0.5f), androidx.compose.ui.geometry.CornerRadius(side * 0.06f), style = stroke)
                drawCircle(tint, radius = side * 0.08f, center = Offset(size.width / 2, size.height / 2))
                drawLine(tint, Offset(side * 0.35f, inset), Offset(side * 0.35f, side * 0.25f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(side * 0.65f, inset), Offset(side * 0.65f, side * 0.25f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(side * 0.35f, side * 0.75f), Offset(side * 0.35f, bottom), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(side * 0.65f, side * 0.75f), Offset(side * 0.65f, bottom), stroke.width, StrokeCap.Round)
            }
            AppGlyphKind.Shield -> {
                val shield = Path().apply {
                    moveTo(side * 0.5f, inset)
                    lineTo(right, side * 0.26f)
                    lineTo(right, side * 0.58f)
                    quadraticTo(right, bottom, side * 0.5f, bottom + side * 0.02f)
                    quadraticTo(inset, bottom, inset, side * 0.58f)
                    lineTo(inset, side * 0.26f)
                    close()
                }
                drawPath(shield, tint, style = stroke)
            }
            AppGlyphKind.Book -> {
                val spine = Path().apply {
                    moveTo(side * 0.5f, inset); lineTo(side * 0.5f, bottom)
                }
                drawPath(spine, tint, style = stroke)
                val leftPage = Path().apply {
                    moveTo(side * 0.5f, inset); lineTo(inset, side * 0.22f); lineTo(inset, bottom); lineTo(side * 0.5f, bottom * 0.92f)
                }
                drawPath(leftPage, tint, style = stroke)
                val rightPage = Path().apply {
                    moveTo(side * 0.5f, inset); lineTo(right, side * 0.22f); lineTo(right, bottom); lineTo(side * 0.5f, bottom * 0.92f)
                }
                drawPath(rightPage, tint, style = stroke)
            }
            AppGlyphKind.Info -> {
                drawCircle(tint, radius = side * 0.38f, center = Offset(size.width / 2, size.height / 2), style = stroke)
                drawCircle(tint, radius = side * 0.05f, center = Offset(size.width / 2, side * 0.33f))
                drawLine(tint, Offset(size.width / 2, side * 0.46f), Offset(size.width / 2, side * 0.68f), stroke.width, StrokeCap.Round)
            }
            AppGlyphKind.History -> {
                drawCircle(tint, radius = side * 0.38f, center = Offset(size.width / 2, size.height / 2), style = stroke)
                drawLine(tint, Offset(size.width / 2, size.height / 2), Offset(size.width / 2, side * 0.26f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(size.width / 2, size.height / 2), Offset(side * 0.70f, size.height / 2), stroke.width, StrokeCap.Round)
            }
            AppGlyphKind.Back -> {
                val chevron = Path().apply {
                    moveTo(side * 0.65f, inset + side * 0.08f)
                    lineTo(side * 0.35f, size.height / 2)
                    lineTo(side * 0.65f, bottom - side * 0.08f)
                }
                drawPath(chevron, tint, style = stroke)
            }
            AppGlyphKind.ArrowRight -> {
                val chevron = Path().apply {
                    moveTo(side * 0.35f, inset + side * 0.08f)
                    lineTo(side * 0.65f, size.height / 2)
                    lineTo(side * 0.35f, bottom - side * 0.08f)
                }
                drawPath(chevron, tint, style = stroke)
            }
        }
    }
}

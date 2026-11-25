package org.bharatai.ps5.metalinspection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bharatai.ps5.metalinspection.ui.theme.CyberTeal
import org.bharatai.ps5.metalinspection.ui.theme.DeepNavy
import org.bharatai.ps5.metalinspection.ui.theme.ElectricCyan
import org.bharatai.ps5.metalinspection.ui.theme.HazardAmber
import org.bharatai.ps5.metalinspection.ui.theme.HighSeverity
import org.bharatai.ps5.metalinspection.ui.theme.LaserCrimson
import org.bharatai.ps5.metalinspection.ui.theme.LowSeverity
import org.bharatai.ps5.metalinspection.ui.theme.MediumSeverity
import org.bharatai.ps5.metalinspection.ui.theme.MetalInspectionAssistantTheme
import org.bharatai.ps5.metalinspection.ui.theme.NeonEmerald
import org.bharatai.ps5.metalinspection.ui.theme.ObsidianCanvas
import org.bharatai.ps5.metalinspection.ui.theme.PrecisionBorder
import org.bharatai.ps5.metalinspection.ui.theme.PrecisionBorderBright
import org.bharatai.ps5.metalinspection.ui.theme.SlateSurface
import org.bharatai.ps5.metalinspection.ui.theme.SlateSurfaceElevated
import org.bharatai.ps5.metalinspection.ui.theme.SlateSurfaceVariant
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmGenerationConfig
import org.pytorch.executorch.extension.llm.LlmModule
import org.pytorch.executorch.extension.llm.LlmModuleConfig
import java.io.File
import java.text.DateFormat
import java.util.Date
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import org.bharatai.ps5.metalinspection.ui.theme.CoolBlue
import org.bharatai.ps5.metalinspection.ui.theme.GraphiteSlate
import org.bharatai.ps5.metalinspection.ui.theme.SteelRecessed
import java.util.Locale

// Synthetic imagery is not permitted for the competition demo. Keep its research
// implementation and artifacts intact, but do not expose the synthetic flow here.
private const val SHOW_SYNTHETIC_RESEARCH_DEMO = false

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        val initialUri: Uri? = intent?.data ?: intent?.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
        setContent {
            MetalInspectionAssistantTheme(darkTheme = true) {
                InspectionApp(initialUri = initialUri)
            }
        }
    }
}

@Composable
private fun InspectionApp(initialUri: Uri? = null) {
    val context = LocalContext.current
    var currentScreen by rememberSaveable {
        mutableStateOf(if (initialUri != null) AppScreen.Inspect else AppScreen.Welcome)
    }
    var history by remember { mutableStateOf<List<InspectionRecord>>(emptyList()) }
    val competitionHistory = history.filterNot { it.label.startsWith("Synthetic fitting:", ignoreCase = true) }

    LaunchedEffect(context, currentScreen) {
        history = withContext(Dispatchers.IO) {
            InspectionHistoryStore(context).use { store -> store.recent() }
        }
    }

    when (currentScreen) {
        AppScreen.Welcome -> {
            WelcomeScreen(
                onStartInspection = { currentScreen = AppScreen.Inspect },
                onViewTutorial = { currentScreen = AppScreen.Tutorial },
                onOpenHistory = { currentScreen = AppScreen.History },
                onOpenAbout = { currentScreen = AppScreen.About },
            )
        }
        AppScreen.Tutorial -> {
            TutorialScreen(
                onDone = { currentScreen = AppScreen.Welcome },
                onStartInspection = { currentScreen = AppScreen.Inspect },
            )
        }
        AppScreen.About -> {
            AboutScreen(
                onBack = { currentScreen = AppScreen.Welcome },
            )
        }
        AppScreen.History -> {
            HistoryScreen(
                records = competitionHistory,
                onBack = { currentScreen = AppScreen.Welcome },
                onStartNewInspection = { currentScreen = AppScreen.Inspect },
            )
        }
        AppScreen.Inspect -> {
            InspectionHomeScreen(
                initialUri = initialUri,
                competitionHistory = competitionHistory,
                onBack = { currentScreen = AppScreen.Welcome },
                onViewTutorial = { currentScreen = AppScreen.Tutorial },
                onOpenHistory = { currentScreen = AppScreen.History },
                onOpenAbout = { currentScreen = AppScreen.About },
                onHistoryChanged = { updated -> history = updated },
            )
        }
    }
}

@Composable
private fun InspectionHomeScreen(
    initialUri: Uri? = null,
    competitionHistory: List<InspectionRecord> = emptyList(),
    onBack: () -> Unit = {},
    onViewTutorial: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onHistoryChanged: (List<InspectionRecord>) -> Unit = {},
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(initialUri) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("Awaiting specimen. Capture or import steel surface image.") }
    var prediction by remember { mutableStateOf<String?>(null) }
    var analysisResult by remember { mutableStateOf<DefectAnalysisResult?>(null) }
    var currentInspectionId by remember { mutableStateOf<Long?>(null) }
    var operatorSeverity by remember { mutableStateOf<String?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var kolektorScore by remember { mutableStateOf<Float?>(null) }
    var checkingKolektor by remember { mutableStateOf(false) }
    var syntheticResult by remember { mutableStateOf<SyntheticFittingPrediction?>(null) }
    var checkingSynthetic by remember { mutableStateOf(false) }
    var syntheticStatus by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                previewBitmap = null
                prediction = null
                analysisResult = null
                kolektorScore = null
                syntheticResult = null
                syntheticStatus = ""
                operatorSeverity = null
                selectedUri = uri
            }
        },
    )

    val cameraCapture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { saved ->
            val uri = cameraUri
            if (saved && uri != null) {
                selectedUri = uri
                prediction = null
                analysisResult = null
                kolektorScore = null
                syntheticResult = null
                syntheticStatus = ""
                operatorSeverity = null
                status = "Specimen acquired. Tap Analyze to classify."
            } else {
                status = "Camera capture cancelled. Try again."
            }
        },
    )

    LaunchedEffect(selectedUri) {
        val uri = selectedUri ?: return@LaunchedEffect
        status = "Loading specimen image…"
        previewBitmap = withContext(Dispatchers.IO) {
            loadPreviewBitmap(context.applicationContext, uri)
        }
        prediction = null
        analysisResult = null
        currentInspectionId = null
        operatorSeverity = null
        kolektorScore = null
        syntheticResult = null
        syntheticStatus = ""
        status = if (previewBitmap != null) {
            "Specimen ready. Tap Analyze to classify."
        } else {
            "Could not load image. Please select another file."
        }
    }

    fun takePhoto() {
        val photo = File.createTempFile("inspection_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)
        cameraUri = uri
        cameraCapture.launch(uri)
    }

    fun chooseSavedImage() {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun analyzeImage() {
        val image = previewBitmap ?: return
        if (analyzing) return
        analyzing = true
        status = "Analyzing on-device edge pipeline…"
        prediction = null
        analysisResult = null
        currentInspectionId = null
        operatorSeverity = null
        val startedAt = SystemClock.elapsedRealtime()
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching { OpenCvDefectClassifier.analyze(context, image) }
            }
            analyzing = false
            result.onSuccess { output ->
                analysisResult = output
                val label = output.label.replace('_', ' ').replace('-', ' ')
                prediction = label
                val densityStr = String.format(Locale.US, "%.1f", output.morphology.defectAreaPercent)
                status = "Inspection complete (edge predict: ${SystemClock.elapsedRealtime() - startedAt} ms · defect density: $densityStr%)."
                val record = InspectionRecord(
                    id = 0,
                    label = label,
                    inspectedAt = System.currentTimeMillis(),
                    durationMs = SystemClock.elapsedRealtime() - startedAt,
                    severity = output.morphology.calculatedSeverity,
                )
                currentInspectionId = withContext(Dispatchers.IO) {
                    InspectionHistoryStore(context).use { store -> store.add(record) }
                }
                val recent = withContext(Dispatchers.IO) {
                    InspectionHistoryStore(context).use { store -> store.recent() }
                }
                onHistoryChanged(recent)
            }.onFailure {
                status = "Inspection failed: ${it.message ?: "OpenCV processing error"}"
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Modern Brushed-Metal Top Bar (No unverified claims)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
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
                            .clickable { onBack() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AppGlyph(AppGlyphKind.Back, Modifier.size(16.dp), Color.White)
                        }
                    }
                    Column {
                        Text(
                            "Surface Inspection",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            "On-Device Defect Metrology",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSurfaceVariant,
                        border = BorderStroke(1.dp, PrecisionBorder),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onViewTutorial() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AppGlyph(AppGlyphKind.Book, Modifier.size(17.dp), ElectricCyan)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSurfaceVariant,
                        border = BorderStroke(1.dp, PrecisionBorder),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onOpenHistory() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AppGlyph(AppGlyphKind.History, Modifier.size(17.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSurfaceVariant,
                        border = BorderStroke(1.dp, PrecisionBorder),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onOpenAbout() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AppGlyph(AppGlyphKind.Info, Modifier.size(17.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Section 1: Capture & Optical Specimen Viewport
            SectionHeading(title = "Specimen Viewport", eyebrow = "01  ·  ACQUISITION")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = metallicBorderStroke(bright = true),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                val bitmap = previewBitmap
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .brushedMetalFinish(elevated = false)
                        .padding(10.dp)
                        .background(SteelRecessed, RoundedCornerShape(16.dp))
                        .border(1.dp, PrecisionBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bitmap == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SlateSurfaceVariant,
                                border = BorderStroke(1.dp, CoolBlue.copy(alpha = 0.35f)),
                            ) {
                                Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                                    AppGlyph(AppGlyphKind.Scan, Modifier.size(26.dp), CoolBlue)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("No Specimen Loaded", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                            Text(
                                "Capture or import a metal surface photo to begin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Box(contentAlignment = Alignment.BottomStart) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Selected steel specimen",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp),
                                contentScale = ContentScale.Fit,
                            )
                            Surface(
                                modifier = Modifier.padding(10.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = GraphiteSlate.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, PrecisionBorder),
                            ) {
                                Text(
                                    "SPECIMEN LOADED · 200x200 ROI",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoolBlue,
                                )
                            }
                        }
                    }
                }
            }

            // Status Readout Pill
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SlateSurfaceVariant,
                border = BorderStroke(1.dp, PrecisionBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppGlyph(
                        if (status.startsWith("Inspection complete")) AppGlyphKind.Check else AppGlyphKind.Scan,
                        Modifier.size(18.dp),
                        if (status.startsWith("Inspection complete")) NeonEmerald else CoolBlue,
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // High-Impact Tactile Action Row
            Button(
                onClick = ::analyzeImage,
                enabled = previewBitmap != null && !analyzing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoolBlue,
                    contentColor = Color(0xFF0B1320),
                    disabledContainerColor = SlateSurfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
            ) {
                AppGlyph(AppGlyphKind.Scan, Modifier.size(19.dp), if (previewBitmap != null) Color(0xFF0B1320) else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(9.dp))
                Text(
                    if (analyzing) "ANALYZING ON-DEVICE…" else "ANALYZE SPECIMEN",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = ::takePhoto,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PrecisionBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    AppGlyph(AppGlyphKind.Camera, Modifier.size(18.dp), CoolBlue)
                    Spacer(Modifier.width(7.dp))
                    Text("Capture", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = ::chooseSavedImage,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PrecisionBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    AppGlyph(AppGlyphKind.Gallery, Modifier.size(18.dp), CoolBlue)
                    Spacer(Modifier.width(7.dp))
                    Text("Import", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Section 2: Defect Classification & Metrology HUD
            prediction?.let { result ->
                fun saveOperatorSeverity(level: String) {
                    operatorSeverity = level
                    val recordId = currentInspectionId ?: return
                    scope.launch {
                        val updatedHistory = withContext(Dispatchers.IO) {
                            InspectionHistoryStore(context).use { store ->
                                store.setSeverity(recordId, severityHistoryValue(level))
                                store.recent()
                            }
                        }
                        onHistoryChanged(updatedHistory)
                    }
                }

                val morph = analysisResult?.morphology
                val areaPct = morph?.defectAreaPercent ?: 0f
                val isAiClean = result.lowercase().contains("clean") || areaPct < 1.0f
                val operatorIsPass = operatorSeverity?.equals("Pass", ignoreCase = true) == true
                val activeSevTier = if (isAiClean || operatorIsPass) "PASS" else (operatorSeverity ?: morph?.calculatedSeverity ?: "MEDIUM").uppercase()
                val effectiveGuidanceLabel = if (isAiClean || operatorIsPass) "clean" else result

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = metallicBorderStroke(bright = true),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .brushedMetalFinish(elevated = true)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        SectionHeading(title = "Inspection Finding", eyebrow = "02  ·  CLASSIFICATION & METROLOGY")

                        // Immediate Quality Disposition Action Bar (Operator Decision)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = { saveOperatorSeverity(morph?.calculatedSeverity?.takeUnless { it.contains("Pass", ignoreCase = true) } ?: "High") },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (!isAiClean && !operatorIsPass) LaserCrimson.copy(alpha = 0.22f) else SlateSurfaceVariant,
                                    contentColor = if (!isAiClean && !operatorIsPass) LaserCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                border = BorderStroke(1.dp, if (!isAiClean && !operatorIsPass) LaserCrimson else PrecisionBorder),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(modifier = Modifier.size(7.dp), shape = CircleShape, color = if (!isAiClean && !operatorIsPass) LaserCrimson else Color.Gray) {}
                                    Text("DEFECT FLAGGED", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            FilledTonalButton(
                                onClick = { saveOperatorSeverity("Pass") },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isAiClean || operatorIsPass) NeonEmerald.copy(alpha = 0.22f) else SlateSurfaceVariant,
                                    contentColor = if (isAiClean || operatorIsPass) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                border = BorderStroke(1.dp, if (isAiClean || operatorIsPass) NeonEmerald else PrecisionBorder),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(modifier = Modifier.size(7.dp), shape = CircleShape, color = if (isAiClean || operatorIsPass) NeonEmerald else Color.Gray) {}
                                    Text("PASS / NO DEFECT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        // Primary Defect Classification & Prominent Severity Banner (ALWAYS OBJECTIVE)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    if (isAiClean) "Nominal Surface (No Flaws Detected)" else "${result.replaceFirstChar { it.uppercase() }} Defect",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = if (isAiClean) NeonEmerald else when (result.lowercase()) {
                                        "crazing" -> LaserCrimson
                                        "scratches" -> CoolBlue
                                        "pitted_surface", "pitted surface" -> HazardAmber
                                        else -> CyberTeal
                                    },
                                )
                                Text(
                                    if (isAiClean) "VISUAL BASELINE: NOMINAL · WITHIN SURFACE TOLERANCE" else "AUTOMATED SURFACE DEFECT DETECTED · METROLOGY ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAiClean) NeonEmerald else CoolBlue,
                                )
                            }

                            // Objective Automated Severity Badge
                            val isHigh = morph?.calculatedSeverity?.contains("High", ignoreCase = true) == true
                            val isMed = morph?.calculatedSeverity?.contains("Medium", ignoreCase = true) == true
                            val sevColor = if (isAiClean) NeonEmerald else if (isHigh) LaserCrimson else if (isMed) HazardAmber else LowSeverity
                            val sevLabel = if (isAiClean) "NOMINAL" else if (isHigh) "HIGH" else if (isMed) "MEDIUM" else "LOW"

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = sevColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.5.dp, sevColor),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(8.dp),
                                            shape = CircleShape,
                                            color = sevColor,
                                        ) {}
                                        Text(
                                            if (isAiClean) "STATUS: NOMINAL" else "● FLAGGED: $sevLabel",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = sevColor,
                                        )
                                    }
                                    if (isAiClean) {
                                        Text(
                                            "Within visual baseline",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = sevColor.copy(alpha = 0.85f),
                                        )
                                    } else {
                                        Text(
                                            "${String.format(Locale.US, "%.1f", areaPct)}% surface density",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = sevColor.copy(alpha = 0.85f),
                                        )
                                    }
                                }
                            }
                        }

                        // Display clear Inspector Override notice if human disposition differs from AI detection
                        if (!isAiClean && operatorIsPass) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = NeonEmerald.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, NeonEmerald),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = NeonEmerald) {}
                                    Column {
                                        Text("OPERATOR DISPOSITION: ACCEPTED / PASS (MANUAL OVERRIDE)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NeonEmerald)
                                        Text("Inspector verified part tolerance and authorized clearance for assembly despite AI defect warning.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SlateSurfaceVariant,
                            border = BorderStroke(1.dp, PrecisionBorder),
                        ) {
                            Text(
                                "NEU-DET Benchmark · 83.70% test accuracy / 0.837 macro F1 on 270 held-out images. Real-time edge inference on Arm Cortex-A.",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Automated Morphological Metrology Dashboard (Solves the Severity Gap!)
                        analysisResult?.let { analysis ->
                            val morph = analysis.morphology
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianCanvas,
                                border = BorderStroke(1.dp, PrecisionBorder),
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            InspectionGlyph(InspectionGlyphKind.Metrology, Modifier.size(16.dp), ElectricCyan)
                                            Text("SURFACE METROLOGY", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when {
                                                morph.defectAreaPercent < 2.5f -> NeonEmerald.copy(alpha = 0.15f)
                                                morph.defectAreaPercent < 8.0f -> HazardAmber.copy(alpha = 0.15f)
                                                else -> LaserCrimson.copy(alpha = 0.15f)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    morph.defectAreaPercent < 2.5f -> NeonEmerald
                                                    morph.defectAreaPercent < 8.0f -> HazardAmber
                                                    else -> LaserCrimson
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = morph.calculatedSeverity,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = when {
                                                    morph.defectAreaPercent < 2.5f -> NeonEmerald
                                                    morph.defectAreaPercent < 8.0f -> HazardAmber
                                                    else -> LaserCrimson
                                                }
                                            )
                                        }
                                    }

                                    // Metrics Grid: Density & Anomaly Regions
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            color = SlateSurface,
                                            border = BorderStroke(1.dp, PrecisionBorder),
                                        ) {
                                            Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("DEFECT DENSITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    String.format(Locale.US, "%.1f%%", morph.defectAreaPercent),
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = ElectricCyan,
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .background(SlateSurfaceVariant, RoundedCornerShape(2.dp))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(fraction = (morph.defectAreaPercent / 100f).coerceIn(0.04f, 1f))
                                                            .height(4.dp)
                                                            .background(
                                                                when {
                                                                    morph.defectAreaPercent < 2.5f -> NeonEmerald
                                                                    morph.defectAreaPercent < 8.0f -> HazardAmber
                                                                    else -> LaserCrimson
                                                                },
                                                                RoundedCornerShape(2.dp)
                                                            )
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            color = SlateSurface,
                                            border = BorderStroke(1.dp, PrecisionBorder),
                                        ) {
                                            Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("ANOMALY REGIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    "${morph.contourCount}",
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                )
                                                Text("micro-contours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    Text(
                                        "Automated surface severity computed deterministically from pixel gradient and defect contour density on-device via OpenCV. Optical decision support only; verify critical tolerances with calibrated physical instruments.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = PrecisionBorder)

                        // Actionable Industrial Recommendations
                        val isDispositionPass = isAiClean || operatorIsPass
                        Text(
                            if (isDispositionPass) "Actionable Guidance · Part Clearance SOP" else "Actionable Guidance · Defect Response SOP",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isDispositionPass) NeonEmerald else Color.White,
                        )
                        checklistFor(if (isDispositionPass) "clean" else result).forEachIndexed { index, item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = SlateSurfaceVariant,
                                border = BorderStroke(1.dp, PrecisionBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(11.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = (if (isDispositionPass) NeonEmerald else ElectricCyan).copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            String.format(Locale.US, "%02d", index + 1),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDispositionPass) NeonEmerald else ElectricCyan,
                                        )
                                    }
                                    Text(item, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFDCE6F2))
                                }
                            }
                        }

                        HorizontalDivider(color = PrecisionBorder)

                        // Inspector Quality Override (Optional Audit Note)
                        Text("Inspector Quality Override (Optional)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            "Record human inspector verification. Use 'Pass / Visual Acceptance' if part meets visual acceptance, or grade defect severity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val rubric = severityRubricFor(result)
                        val isPassSelected = operatorIsPass || (operatorSeverity == null && isAiClean)
                        SeverityChoice("Pass / Visual Acceptance", "Specimen is clear of surface flaws; satisfies visual baseline criteria for release.", isPassSelected, NeonEmerald) { saveOperatorSeverity("Pass") }
                        SeverityChoice("Low Severity", rubric.low, !isPassSelected && (operatorSeverity == "Low" || (operatorSeverity == null && morph?.calculatedSeverity?.contains("Low", ignoreCase = true) == true)), LowSeverity) { saveOperatorSeverity("Low") }
                        SeverityChoice("Medium Severity", rubric.medium, !isPassSelected && (operatorSeverity == "Medium" || (operatorSeverity == null && morph?.calculatedSeverity?.contains("Medium", ignoreCase = true) == true)), MediumSeverity) { saveOperatorSeverity("Medium") }
                        SeverityChoice("High Severity", rubric.high, !isPassSelected && (operatorSeverity == "High" || (operatorSeverity == null && morph?.calculatedSeverity?.contains("High", ignoreCase = true) == true)), HighSeverity) { saveOperatorSeverity("High") }
                        Text(
                            if (isPassSelected) "Active Status: VISUAL ACCEPTANCE RECORDED"
                            else operatorSeverity?.let { "Recorded Override: $it" } ?: "Active Status: FLAGGED · Using Automated Severity (${morph?.calculatedSeverity ?: "CALCULATING"})",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isPassSelected) NeonEmerald else if (operatorSeverity != null) ElectricCyan else LaserCrimson,
                        )
                    }
                }

                // Llama 3.2 ExecuTorch Assistant Terminal
                LlamaAssistantPanel(context, effectiveGuidanceLabel, activeSevTier)

                Spacer(modifier = Modifier.height(4.dp))

                // Experimental Anomaly Binary Check
                FilledTonalButton(
                    enabled = previewBitmap != null && !checkingKolektor,
                    onClick = {
                        val image = previewBitmap ?: return@FilledTonalButton
                        checkingKolektor = true
                        val startedAt = SystemClock.elapsedRealtime()
                        scope.launch {
                            val resultScore = withContext(Dispatchers.Default) {
                                runCatching { KolektorBinaryDetector.defectScore(context, image) }
                            }
                            checkingKolektor = false
                            resultScore.onSuccess { score ->
                                kolektorScore = score
                                val defectLike = score >= KolektorBinaryDetector.defectThreshold
                                val signalLabel = if (defectLike) "defect-like" else "normal-like"
                                val duration = SystemClock.elapsedRealtime() - startedAt
                                withContext(Dispatchers.IO) {
                                    InspectionHistoryStore(context).use { store ->
                                        store.add(
                                            InspectionRecord(
                                                id = 0,
                                                label = "KolektorSDD2 experimental: $signalLabel (score ${(score * 100).toInt()}%)",
                                                inspectedAt = System.currentTimeMillis(),
                                                durationMs = duration,
                                                severity = "unreviewed",
                                            ),
                                        )
                                    }
                                    val updated = InspectionHistoryStore(context).use { store -> store.recent() }
                                    onHistoryChanged(updated)
                                }
                                status = "Experimental binary check complete (on-device)."
                            }.onFailure {
                                status = "Could not run binary check: ${it.message ?: "OpenCV/model error"}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SlateSurfaceVariant,
                        contentColor = ElectricCyan,
                    ),
                ) {
                    Text(if (checkingKolektor) "CHECKING ANOMALY SIGNAL…" else "RUN EXPERIMENTAL NORMAL/DEFECT CHECK", style = MaterialTheme.typography.labelSmall)
                }

                kolektorScore?.let { score ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                        border = BorderStroke(1.dp, PrecisionBorder),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Experimental Binary Signal", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "${if (score >= KolektorBinaryDetector.defectThreshold) "Defect-like" else "Normal-like"} — model score ${(score * 100).toInt()}% (threshold ${(KolektorBinaryDetector.defectThreshold * 100).toInt()}%).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (score >= KolektorBinaryDetector.defectThreshold) LaserCrimson else NeonEmerald,
                            )
                        }
                    }
                }
            }

            // Quick Ledger Link
            if (competitionHistory.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenHistory() },
                    shape = RoundedCornerShape(14.dp),
                    color = SlateSurface,
                    border = BorderStroke(1.dp, PrecisionBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppGlyph(AppGlyphKind.History, Modifier.size(18.dp), CoolBlue)
                            Column {
                                Text("Inspection Ledger", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text("${competitionHistory.size} records preserved locally in SQLite", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("View all →", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = CoolBlue)
                    }
                }
            }

            // Trust & Decision Support Footer Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = SteelRecessed,
                border = BorderStroke(1.dp, PrecisionBorder),
            ) {
                Text(
                    text = "Visual decision support only. Model predictions and morphological severity indices assist human inspectors and do not replace certified metallurgical or structural acceptance standards.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, eyebrow: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

private enum class InspectionGlyphKind { Scan, Camera, Gallery, Check, Metrology, Cpu }

@Composable
private fun InspectionGlyph(kind: InspectionGlyphKind, modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val side = size.minDimension
        val inset = side * 0.12f
        val right = size.width - inset
        val bottom = size.height - inset
        val stroke = Stroke(width = side * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            InspectionGlyphKind.Scan -> {
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
            InspectionGlyphKind.Camera -> {
                val body = Path().apply {
                    moveTo(inset, side * 0.34f); lineTo(side * 0.36f, side * 0.34f)
                    lineTo(side * 0.45f, side * 0.22f); lineTo(side * 0.67f, side * 0.22f)
                    lineTo(side * 0.76f, side * 0.34f); lineTo(right, side * 0.34f)
                    lineTo(right, bottom); lineTo(inset, bottom); close()
                }
                drawPath(body, tint, style = stroke)
                drawCircle(tint, radius = side * 0.18f, center = Offset(size.width / 2, side * 0.63f), style = stroke)
            }
            InspectionGlyphKind.Gallery -> {
                drawRoundRect(tint, Offset(inset, inset), Size(side * 0.72f, side * 0.72f), androidx.compose.ui.geometry.CornerRadius(side * 0.08f), style = stroke)
                drawCircle(tint, radius = side * 0.065f, center = Offset(side * 0.34f, side * 0.34f))
                val hills = Path().apply {
                    moveTo(side * 0.18f, side * 0.78f); lineTo(side * 0.43f, side * 0.52f)
                    lineTo(side * 0.57f, side * 0.66f); lineTo(side * 0.68f, side * 0.55f)
                    lineTo(side * 0.86f, side * 0.74f)
                }
                drawPath(hills, tint, style = stroke)
            }
            InspectionGlyphKind.Check -> {
                drawCircle(tint, radius = side * 0.39f, center = Offset(size.width / 2, size.height / 2), style = stroke)
                val check = Path().apply {
                    moveTo(side * 0.28f, side * 0.51f); lineTo(side * 0.44f, side * 0.66f); lineTo(side * 0.73f, side * 0.35f)
                }
                drawPath(check, tint, style = stroke)
            }
            InspectionGlyphKind.Metrology -> {
                drawLine(tint, Offset(inset, side * 0.3f), Offset(right, side * 0.3f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(inset, side * 0.7f), Offset(right, side * 0.7f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(side * 0.3f, side * 0.3f), Offset(side * 0.3f, side * 0.7f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(side * 0.7f, side * 0.3f), Offset(side * 0.7f, side * 0.7f), stroke.width, StrokeCap.Round)
            }
            InspectionGlyphKind.Cpu -> {
                drawRoundRect(tint, Offset(side * 0.25f, side * 0.25f), Size(side * 0.5f, side * 0.5f), androidx.compose.ui.geometry.CornerRadius(side * 0.06f), style = stroke)
                drawCircle(tint, radius = side * 0.08f, center = Offset(size.width / 2, size.height / 2))
            }
        }
    }
}

@Composable
private fun SeverityChoice(
    title: String,
    cue: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else PrecisionBorder,
                shape = shape,
            )
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.09f) else SlateSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = accent) {}
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
                Text(cue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Surface(shape = RoundedCornerShape(4.dp), color = accent.copy(alpha = 0.15f)) {
                    Text("ACTIVE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
        }
    }
}

@Composable
private fun LlamaAssistantPanel(context: Context, defectLabel: String?, severityTier: String = "HIGH") {
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val llamaDirectory = remember(context) { File(context.filesDir, "llama") }
    var modelPath by remember(llamaDirectory) {
        mutableStateOf(File(llamaDirectory, "Llama-3.2-1B-Instruct.pte").takeIf { LlamaModelStore.isValidModel(it) }?.absolutePath)
    }
    var tokenizerPath by remember(llamaDirectory) {
        mutableStateOf(File(llamaDirectory, "tokenizer.model").takeIf { LlamaModelStore.isValidTokenizer(it) }?.absolutePath)
    }
    var module by remember { mutableStateOf<LlmModule?>(null) }
    var loadingModel by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }
    val checklistText = defectLabel?.let { label ->
        checklistFor(label).joinToString("\n") { "- $it" }
    }.orEmpty()
    var panelMessage by remember {
        mutableStateOf(
            if (modelPath != null && tokenizerPath != null) "Complete verified Llama 3.2 model & tokenizer detected on phone. Load to generate advisory."
            else if (File(llamaDirectory, "Llama-3.2-1B-Instruct.pte").exists() || File(llamaDirectory, "tokenizer.model").exists()) "Saved Llama files are incomplete or mismatched. Select the complete downloaded model and matching tokenizer again."
            else "Select the complete ExecuTorch model and matching tokenizer. Incomplete copies are rejected before native loading."
        )
    }

    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            panelMessage = "Copying model to app storage…"
            runCatching { LlamaModelStore.copyIntoPrivateStorage(context, uri, "Llama-3.2-1B-Instruct.pte") }
                .onSuccess { modelPath = it.absolutePath; panelMessage = "Model saved on this phone." }
                .onFailure { panelMessage = "Could not copy model: ${it.message}" }
        }
    }
    val tokenizerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            panelMessage = "Copying tokenizer…"
            runCatching { LlamaModelStore.copyIntoPrivateStorage(context, uri, "tokenizer.model") }
                .onSuccess { tokenizerPath = it.absolutePath; panelMessage = "Tokenizer saved on this phone." }
                .onFailure { panelMessage = "Could not copy tokenizer: ${it.message}" }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = metallicBorderStroke(bright = false),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .brushedMetalFinish(elevated = false)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeading("AI Advisory Copilot", "04  ·  EXECUTORCH + LLAMA 3.2")
            Text(
                "On-device reasoning model generates structured operator advisory from predicted findings. Runs 100% offline via ExecuTorch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = ObsidianCanvas,
                border = BorderStroke(1.dp, PrecisionBorder),
            ) {
                Text(
                    panelMessage,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                )
            }
            if (module == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        enabled = !loadingModel,
                        onClick = { modelPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrecisionBorder),
                    ) { Text("Model File", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        enabled = !loadingModel,
                        onClick = { tokenizerPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrecisionBorder),
                    ) { Text("Tokenizer", style = MaterialTheme.typography.labelSmall) }
                }
            }
            val hasFiles = modelPath != null && tokenizerPath != null
            val isReady = module != null
            
            Button(
                enabled = hasFiles && defectLabel != null && !loadingModel,
                onClick = {
                    val mp = modelPath ?: return@Button
                    val tp = tokenizerPath ?: return@Button
                    val label = defectLabel ?: return@Button
                    answer = ""
                    
                    scope.launch {
                        val activeModule = module ?: run {
                            loadingModel = true
                            panelMessage = "Initializing ExecuTorch Llama on-device…"
                            val loadResult = withContext(Dispatchers.IO) {
                                runCatching {
                                    LlamaModelStore.verifyPair(File(mp), File(tp))
                                    val config = LlmModuleConfig.create()
                                        .modulePath(mp)
                                        .tokenizerPath(tp)
                                        .temperature(0.2f)
                                        .modelType(LlmModuleConfig.MODEL_TYPE_TEXT)
                                        .loadMode(LlmModuleConfig.LOAD_MODE_MMAP)
                                        .build()
                                    LlmModule(config).also { it.load() }
                                }
                            }
                            loadingModel = false
                            loadResult.fold(
                                onSuccess = { loaded ->
                                    module = loaded
                                    panelMessage = "ExecuTorch loaded. Generating recommendation…"
                                    loaded
                                },
                                onFailure = {
                                    panelMessage = "Model load failed: ${it.message ?: "file error"}"
                                    null
                                }
                            )
                        } ?: return@launch
                        
                        panelMessage = "Generating on-device inference via ExecuTorch…"
                        val fullPrompt = buildLlamaPrompt(label, checklistText, severityTier)
                        withContext(Dispatchers.IO) {
                            runCatching {
                                activeModule.resetContext()
                                val generationConfig = LlmGenerationConfig.create()
                                    .seqLen(128)
                                    .maxNewTokens(50)
                                    .temperature(0.1f)
                                    .echo(false)
                                    .build()
                                activeModule.generate(fullPrompt, generationConfig, object : LlmCallback {
                                    override fun onResult(result: String) { mainHandler.post { answer = (answer + result).take(500) } }
                                    override fun onStats(stats: String) { mainHandler.post { panelMessage = "Advisory generated on-device." } }
                                    override fun onError(errorCode: Int, message: String) { mainHandler.post { answer = ""; panelMessage = "Llama generation failed: $message" } }
                                })
                            }.onFailure {
                                mainHandler.post {
                                    answer = ""
                                    panelMessage = "Llama execution failed: ${it.message}"
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF00222A),
                ),
            ) {
                Text(
                    when {
                        loadingModel -> "INITIALIZING EXECUTORCH RUNTIME…"
                        !hasFiles -> "SELECT MODEL & TOKENIZER ABOVE"
                        isReady -> "GENERATE LLAMA ADVISORY"
                        else -> "INITIALIZE & GENERATE ADVISORY"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            val displayAnswer = cleanLlamaOutput(answer)
            if (displayAnswer.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCanvas,
                    border = BorderStroke(1.dp, PrecisionBorderBright),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("LLAMA 3.2 ADVISORY OUTPUT", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                        Text(displayAnswer, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun checklistFor(label: String): List<String> = when (label.lowercase().replace(' ', '_').replace('-', '_')) {
    "crazing", "crack", "cracks" -> listOf(
        "IMMEDIATE DISPOSITION: Quarantine coil immediately (Red Tag). Halt uncoiler feed if micro-fissure propagation is continuous.",
        "CORRECTIVE ACTION: Inspect hot-strip mill cooling sprays and work-roll thermal profile for thermal shock.",
        "QUALITY CHECK: Perform ultrasonic / dye-penetrant depth check; verify micro-fissure depth within drawing tolerance."
    )
    "scratches", "scratch" -> listOf(
        "IMMEDIATE DISPOSITION: Hold part for surface dressing. Verify score groove is within allowable 0.05 mm polish tolerance.",
        "CORRECTIVE ACTION: Clean uncoiler guide rollers, entry table, and side bridles for embedded metal shaving grit.",
        "QUALITY CHECK: Verify scratch does not traverse critical sealing surfaces, threads, or stress concentration zones."
    )
    "pitted_surface", "corrosion", "corroded", "pitted" -> listOf(
        "IMMEDIATE DISPOSITION: Isolate batch for surface profilometry. Rework via orbital buffing if pit depression < 0.10 mm.",
        "CORRECTIVE ACTION: Test acid pickling concentration, rinse bath temperature, and neutralize chemical carryover.",
        "QUALITY CHECK: Measure surface roughness (verify Ra <= 1.6 um); ensure no localized flaking or through-thickness porosity."
    )
    "inclusion" -> listOf(
        "IMMEDIATE DISPOSITION: Segregate specimen for metallurgical evaluation. Prevent feeding to high-stress forming line.",
        "CORRECTIVE ACTION: Cross-reference melt heat number; verify upstream tundish deoxidation and argon shielding logs.",
        "QUALITY CHECK: Perform non-destructive magnetic particle inspection across critical load-bearing cross-sections."
    )
    "rolled_in_scale", "rolled-in_scale", "scale" -> listOf(
        "IMMEDIATE DISPOSITION: Crop defective linear strip footage. Do not release to cold reduction or galvanizing lines.",
        "CORRECTIVE ACTION: Inspect high-pressure descaling header nozzles for clogging; verify minimum 150 bar spray pressure.",
        "QUALITY CHECK: Conduct cross-hatch bend test to verify scale entrapment does not cause coating delamination."
    )
    "patches" -> listOf(
        "IMMEDIATE DISPOSITION: Flag sheet for localized rotary grinding or shear cropping. Check adjacent strip for periodic repeats.",
        "CORRECTIVE ACTION: Inspect roughing mill work-roll surface for spalling, roll fatigue, or mechanical roll banding.",
        "QUALITY CHECK: Use ultrasonic thickness gauge to confirm remaining wall thickness after surface blending satisfies minimum gauge."
    )
    "clean", "normal", "no_defect", "passed", "pass", "clean_surface" -> listOf(
        "VISUAL DISPOSITION: NO ANOMALIES DETECTED. Specimen satisfies visual baseline criteria; proceed to standard QA signoff.",
        "CORRECTIVE ACTION: No machine adjustment required. Log specimen evaluation in production QA lot register.",
        "QUALITY CHECK: Continue scheduled interval visual audit on subsequent production components."
    )
    else -> listOf(
        "IMMEDIATE DISPOSITION: Hold specimen under inspection hold until qualified QA inspector completes visual evaluation.",
        "CORRECTIVE ACTION: Check production tooling, lubrication flow, and optical sensor alignment.",
        "QUALITY CHECK: Compare specimen surface against visual master reference standard."
    )
}

internal fun sopActionSummaryFor(label: String): String = when (label.lowercase().replace(' ', '_').replace('-', '_')) {
    "crazing", "crack", "cracks" -> "Quarantine coil with Red Tag immediately and inspect mill roll cooling sprays for thermal shock."
    "scratches", "scratch" -> "Hold part for surface dressing polish (max 0.05mm) and clean guide rollers to remove metal grit."
    "pitted_surface", "corrosion", "corroded", "pitted" -> "Isolate batch for orbital buffing rework (<0.10mm) and check acid pickling bath concentration."
    "inclusion" -> "Segregate part to prevent forming fractures and verify melt furnace argon shielding logs."
    "rolled_in_scale", "rolled-in_scale", "scale" -> "Crop defective linear strip footage and unclog 150-bar descaling nozzles."
    "patches" -> "Flag sheet for shear cropping/grinding and inspect roughing rolls for fatigue spalling."
    "clean", "normal", "no_defect", "passed", "pass", "clean_surface" -> "No surface anomalies detected within visual baseline; proceed to standard QA signoff."
    else -> "Hold specimen for QA verification and check tooling calibration."
}

internal fun buildLlamaPrompt(label: String, approvedChecklist: String, severity: String = "HIGH"): String {
    val cleanLabel = label.replace('_', ' ').replace('-', ' ').trim()
    val cleanSeverity = severity.replace(Regex("\\([^)]*\\)"), "").trim().ifBlank { "High" }
    val isNormal = cleanLabel.lowercase().contains("clean") || cleanLabel.lowercase().contains("normal") || cleanLabel.lowercase().contains("pass")
    val roleDirective = if (isNormal) "Instruct the operator to release the part to production." else "Instruct the operator on part quarantine/rework and machine tooling fix."
    val standardSop = sopActionSummaryFor(cleanLabel)
    val findingTitle = if (isNormal) "Clean / No Defect" else cleanLabel
    val sevTitle = if (isNormal) "PASS" else cleanSeverity
    return """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Industrial QC Copilot. $roleDirective
<|eot_id|><|start_header_id|>user<|end_header_id|>
Finding: $findingTitle (Severity: $sevTitle).
Standard Action: $standardSop
What should the operator do?
<|eot_id|><|start_header_id|>assistant<|end_header_id|>""".trimIndent()
}

internal fun cleanLlamaOutput(text: String): String {
    var cleaned = text.replace(Regex("""<\|[^|]+\|>"""), " ").trim()
    cleaned = cleaned.replace(Regex("""(?i)^(Here is (the )?[^:]+:\s*)+"""), "")
    cleaned = cleaned.replace(Regex("""(?i)^(Recommendation:\s*)+"""), "")
    cleaned = cleaned.replace(Regex("""(?i)^(Action:\s*)+"""), "")
    return cleaned.replace(Regex("\\s+"), " ").trim()
}

internal fun isUnsupportedLlamaOutput(text: String, approvedChecklist: String): Boolean {
    val response = text.lowercase()
    val unsupportedClaims = listOf(
        "image itself", "i can see", "i see a", "the image shows", "computer vision",
        "segment", "segmentation", "bounding box", "mask", "outline the defect",
        "restate", "previous sentence", "revised sentence", "the image is",
    )
    if (unsupportedClaims.any(response::contains)) return true
    val words = response.split(Regex("[^a-z0-9]+")).filter(String::isNotBlank)
    if (words.size !in 5..50) return true
    val genericWords = setOf(
        "check", "checks", "inspect", "inspector", "inspectors", "review", "verify", "results", "result",
        "qualified", "part", "image", "photo", "perform", "action", "steps", "checklist", "guidance",
        "above", "following", "ensure", "use", "should", "could", "would", "the", "and", "with", "for",
        "from", "that", "this", "then", "have", "make", "must", "need", "complete", "appropriate",
    )
    val specificChecklistWords = approvedChecklist.lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 4 && it !in genericWords }
        .toSet()
    if (words.none { it in specificChecklistWords }) return true
    val hasRepeatedThreeWordPhrase = words.windowed(3).groupingBy { it.joinToString(" ") }
        .eachCount().values.any { it >= 3 }
    if (hasRepeatedThreeWordPhrase) return true
    val requiredCues = listOf(
        "inspector", "inspect", "verify", "check", "retake", "recheck", "compare", "capture", "use",
        "quarantine", "release", "rework", "halt", "hold", "clean", "pass", "tolerance", "spray", "roll",
    )
    return requiredCues.none { response.contains(it) }
}

private fun loadPreviewBitmap(context: Context, uri: Uri): Bitmap? {
    fun openStream(): java.io.InputStream? = try {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            File(path).inputStream()
        } else {
            context.contentResolver.openInputStream(uri)
        }
    } catch (_: Throwable) { null }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openStream()?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    val maxDimension = 1280
    while (bounds.outWidth / sampleSize > maxDimension ||
        bounds.outHeight / sampleSize > maxDimension
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return openStream()?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
}

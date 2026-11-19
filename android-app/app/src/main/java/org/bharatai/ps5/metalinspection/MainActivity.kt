package org.bharatai.ps5.metalinspection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import java.text.DateFormat
import java.util.Date
import androidx.core.content.FileProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmGenerationConfig
import org.pytorch.executorch.extension.llm.LlmModule
import org.pytorch.executorch.extension.llm.LlmModuleConfig
import java.io.File
import kotlinx.coroutines.withContext
import org.bharatai.ps5.metalinspection.ui.theme.DeepNavy
import org.bharatai.ps5.metalinspection.ui.theme.HighSeverity
import org.bharatai.ps5.metalinspection.ui.theme.LowSeverity
import org.bharatai.ps5.metalinspection.ui.theme.MediumSeverity
import org.bharatai.ps5.metalinspection.ui.theme.MetalInspectionAssistantTheme

// Synthetic imagery is not permitted for the competition demo. Keep its research
// implementation and artifacts intact, but do not expose the synthetic flow here.
private const val SHOW_SYNTHETIC_RESEARCH_DEMO = false

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MetalInspectionAssistantTheme {
                InspectionHomeScreen()
            }
        }
    }
}

@Composable
private fun InspectionHomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("Choose a saved steel image to begin.") }
    var prediction by remember { mutableStateOf<String?>(null) }
    var currentInspectionId by remember { mutableStateOf<Long?>(null) }
    var operatorSeverity by remember { mutableStateOf<String?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var kolektorScore by remember { mutableStateOf<Float?>(null) }
    var checkingKolektor by remember { mutableStateOf(false) }
    var syntheticResult by remember { mutableStateOf<SyntheticFittingPrediction?>(null) }
    var checkingSynthetic by remember { mutableStateOf(false) }
    var syntheticStatus by remember { mutableStateOf("") }
    var history by remember { mutableStateOf<List<InspectionRecord>>(emptyList()) }
    val competitionHistory = history.filterNot { it.label.startsWith("Synthetic fitting:", ignoreCase = true) }

    LaunchedEffect(context) {
        history = withContext(Dispatchers.IO) {
            InspectionHistoryStore(context).use { store -> store.recent() }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                previewBitmap = null
                prediction = null
                kolektorScore = null
                syntheticResult = null
                syntheticStatus = ""
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
                kolektorScore = null
                syntheticResult = null
                syntheticStatus = ""
                status = "Photo captured. Tap Analyze image to classify it."
            } else {
                status = "No photo captured. Try again."
            }
        },
    )

    LaunchedEffect(selectedUri) {
        val uri = selectedUri ?: return@LaunchedEffect
        status = "Loading image…"
        previewBitmap = withContext(Dispatchers.IO) {
            loadPreviewBitmap(context.applicationContext, uri)
        }
        prediction = null
        currentInspectionId = null
        operatorSeverity = null
        kolektorScore = null
        syntheticResult = null
        syntheticStatus = ""
        status = if (previewBitmap != null) {
            "Image ready. Tap Analyze image to classify it."
        } else {
            "Could not open that image. Please choose another."
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
        status = "Analyzing on this device…"
        prediction = null
        currentInspectionId = null
        operatorSeverity = null
        val startedAt = SystemClock.elapsedRealtime()
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching { OpenCvDefectClassifier.predict(context, image) }
            }
            analyzing = false
            result.onSuccess { detectedLabel ->
                val label = detectedLabel.replace('_', ' ').replace('-', ' ')
                prediction = label
                status = "Analysis complete (on-device)."
                val record = InspectionRecord(
                    id = 0,
                    label = label,
                    inspectedAt = System.currentTimeMillis(),
                    durationMs = SystemClock.elapsedRealtime() - startedAt,
                    severity = "unreviewed",
                )
                currentInspectionId = withContext(Dispatchers.IO) {
                    InspectionHistoryStore(context).use { store -> store.add(record) }
                }
                history = withContext(Dispatchers.IO) {
                    InspectionHistoryStore(context).use { store -> store.recent() }
                }
            }.onFailure {
                status = "Could not analyze image: ${it.message ?: "OpenCV/model error"}"
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.13f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("MI", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("METAL QUALITY CONTROL", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB9C9E2))
                        Text("Inspection assistant", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text("On-device visual inspection", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD0D9E6))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF1E766D)) {
                        Text("ON-DEVICE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                    Text("Research prototype · not an acceptance decision", color = Color(0xFFD0D9E6), style = MaterialTheme.typography.bodySmall)
                }
            }

            SectionHeading(title = "New inspection", eyebrow = "01  ·  CAPTURE")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                val bitmap = previewBitmap
                if (bitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(10.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                                    InspectionGlyph(InspectionGlyphKind.Gallery, Modifier.size(25.dp), MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Text("Add a surface image", style = MaterialTheme.typography.titleMedium)
                            Text("Choose a photo or take one with your camera", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Selected metal surface image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .padding(10.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (previewBitmap != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InspectionGlyph(
                        if (status.startsWith("Analysis complete")) InspectionGlyphKind.Check else InspectionGlyphKind.Scan,
                        Modifier.size(20.dp),
                        if (status.startsWith("Analysis complete")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Button(
                onClick = ::analyzeImage,
                enabled = previewBitmap != null && !analyzing,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                InspectionGlyph(InspectionGlyphKind.Scan, Modifier.size(19.dp), MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(9.dp))
                Text(if (analyzing) "Analyzing…" else "Analyze image", style = MaterialTheme.typography.labelLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = ::takePhoto, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(15.dp)) {
                    InspectionGlyph(InspectionGlyphKind.Camera, Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(7.dp))
                    Text("Take photo", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(onClick = ::chooseSavedImage, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(15.dp)) {
                    InspectionGlyph(InspectionGlyphKind.Gallery, Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(7.dp))
                    Text("Gallery", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (SHOW_SYNTHETIC_RESEARCH_DEMO) {
                Button(
                enabled = previewBitmap != null && !checkingSynthetic,
                onClick = {
                    val image = previewBitmap ?: return@Button
                    prediction = null
                    kolektorScore = null
                    syntheticResult = null
                    checkingSynthetic = true
                    syntheticStatus = "Running the synthetic fitting model on this device…"
                    val startedAt = SystemClock.elapsedRealtime()
                    scope.launch {
                        val result = withContext(Dispatchers.Default) {
                            runCatching { SyntheticFittingDetector.predict(context, image) }
                        }
                        checkingSynthetic = false
                        result.onSuccess { output ->
                            syntheticResult = output
                            syntheticStatus = "Synthetic fitting analysis complete (on-device)."
                            val elapsed = SystemClock.elapsedRealtime() - startedAt
                            val severityForLog = output.severity?.let { "$it (synthetic estimate)" } ?: "not applicable"
                            withContext(Dispatchers.IO) {
                                InspectionHistoryStore(context).use { store ->
                                    store.add(
                                        InspectionRecord(
                                            id = 0,
                                            label = "Synthetic fitting: ${output.label}",
                                            inspectedAt = System.currentTimeMillis(),
                                            durationMs = elapsed,
                                            severity = severityForLog,
                                        ),
                                    )
                                    history = store.recent()
                                }
                            }
                        }.onFailure {
                            syntheticStatus = "Could not run the synthetic fitting demo: ${it.message ?: "OpenCV/model error"}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (checkingSynthetic) "Checking synthetic model…" else "Run synthetic fitting demo")
                }
                Text(
                "This synthetic-only model was trained on 430 generated fitting examples. On 65 held-out generated parts, class accuracy was 95.4%; ordinal severity accuracy was 68.8% on 48 generated defective parts. Low and Medium were each correct on 14/16 parts; High was correct on 5/16. Three of five held-out unusable photos were misclassified. Severity is a team-defined visual tier, not a physical measurement. Use only to demonstrate the app flow, never to accept or reject a real part.",
                style = MaterialTheme.typography.bodySmall,
                )
                if (syntheticStatus.isNotBlank()) Text(syntheticStatus, style = MaterialTheme.typography.bodyMedium)
                syntheticResult?.let { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("AI-generated fitting demo result", style = MaterialTheme.typography.titleMedium)
                        Text(result.label.replace('_', ' '), style = MaterialTheme.typography.headlineSmall)
                        Text("Uncalibrated class score: ${(result.classScore * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        if (result.severity != null) {
                            Text("Team-defined synthetic severity estimate: ${result.severity}", style = MaterialTheme.typography.titleSmall)
                            Text("Uncalibrated severity score: ${((result.severityScore ?: 0f) * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Severity: not applicable for this predicted class.", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Manual rechecks (not acceptance criteria)", style = MaterialTheme.typography.titleSmall)
                        syntheticChecklistFor(result.label).forEach { Text("• $it") }
                        Text("Synthetic-only result. Severity and class labels are not verified for physical fittings. Ask a qualified inspector to assess real parts.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }

            prediction?.let { result ->
                fun saveOperatorSeverity(level: String) {
                    val recordId = currentInspectionId ?: return
                    operatorSeverity = level
                    scope.launch {
                        val updatedHistory = withContext(Dispatchers.IO) {
                            InspectionHistoryStore(context).use { store ->
                                store.setSeverity(recordId, severityHistoryValue(level))
                                store.recent()
                            }
                        }
                        history = updatedHistory
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        SectionHeading(title = "Inspection result", eyebrow = "02  ·  MODEL FINDING")
                        Text(result.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                        Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                "Experimental six-class model · 83.7% held-out accuracy on 270 NEU-DET images. Not validated for production or plumbing parts.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "The model always chooses one of six defect labels. It cannot confirm a defect, identify a good part, reject unrelated images, grade severity, or locate a defect. Treat the result as uncertain and ask an inspector to review it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        Text("Suggested rechecks", style = MaterialTheme.typography.titleMedium)
                        checklistFor(result).forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
                                Text("•", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleMedium)
                                Text(item, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        Text("Operator visual triage", style = MaterialTheme.typography.titleMedium)
                        Text("Choose a visual tier yourself. This is not an AI severity prediction or acceptance decision.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val rubric = severityRubricFor(result)
                        SeverityChoice("Low", rubric.low, operatorSeverity == "Low", LowSeverity) { saveOperatorSeverity("Low") }
                        SeverityChoice("Medium", rubric.medium, operatorSeverity == "Medium", MediumSeverity) { saveOperatorSeverity("Medium") }
                        SeverityChoice("High", rubric.high, operatorSeverity == "High", HighSeverity) { saveOperatorSeverity("High") }
                        Text(
                            operatorSeverity?.let { "Recorded: $it visual tier" } ?: "No visual tier recorded",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                LlamaAssistantPanel(context, result)

                Spacer(modifier = Modifier.height(8.dp))
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
                                    InspectionHistoryStore(context).use { store -> history = store.recent() }
                                }
                                status = "Experimental binary check complete (on-device)."
                            }.onFailure {
                                status = "Could not run the experimental binary check: ${it.message ?: "OpenCV/model error"}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Text(if (checkingKolektor) "Checking…" else "Run experimental normal/defect check")
                }
                Text(
                    "Optional KolektorSDD2-trained check. Its official test result was 82.7% defect recall and 86.7% precision on one controlled product domain. It is not validated for NEU-DET strips, plumbing fittings, or unrelated images; the score is not calibrated and must not be used to accept/reject parts.",
                    style = MaterialTheme.typography.bodySmall,
                )
                kolektorScore?.let { score ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Experimental normal/defect signal", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${if (score >= KolektorBinaryDetector.defectThreshold) "Defect-like" else "Normal-like"} — model score ${(score * 100).toInt()}% (threshold ${(KolektorBinaryDetector.defectThreshold * 100).toInt()}%).",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text("This model has no unrelated-image class and has only been measured on KolektorSDD2 images.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (competitionHistory.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeading("Recent inspections", "LOCAL HISTORY")
                        competitionHistory.forEach { record ->
                            val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(Date(record.inspectedAt))
                            val severityNote = record.severity.takeUnless { it == "unreviewed" }
                                ?.let { " • severity $it" }.orEmpty()
                            Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(record.label, style = MaterialTheme.typography.labelLarge)
                                        Text("$time$severityNote", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("${record.durationMs} ms", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Text(
                    text = "Research prototype. The model may misclassify images and cannot recognize good parts. Recommendations are prompts, not inspection standards; have a qualified inspector make the final decision.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, eyebrow: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private enum class InspectionGlyphKind { Scan, Camera, Gallery, Check }

/** Small, consistent line icons drawn locally so the app needs no added icon library. */
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
                drawLine(tint, androidx.compose.ui.geometry.Offset(inset, inset + corner), androidx.compose.ui.geometry.Offset(inset, inset), stroke.width, StrokeCap.Round)
                drawLine(tint, androidx.compose.ui.geometry.Offset(inset, inset), androidx.compose.ui.geometry.Offset(inset + corner, inset), stroke.width, StrokeCap.Round)
                drawLine(tint, androidx.compose.ui.geometry.Offset(right - corner, inset), androidx.compose.ui.geometry.Offset(right, inset), stroke.width, StrokeCap.Round)
                drawLine(tint, androidx.compose.ui.geometry.Offset(right, inset), androidx.compose.ui.geometry.Offset(right, inset + corner), stroke.width, StrokeCap.Round)
                drawLine(tint, androidx.compose.ui.geometry.Offset(inset, bottom - corner), androidx.compose.ui.geometry.Offset(inset, bottom), stroke.width, StrokeCap.Round)
                drawLine(tint, androidx.compose.ui.geometry.Offset(inset, bottom), androidx.compose.ui.geometry.Offset(inset + corner, bottom), stroke.width, StrokeCap.Round)
                drawLine(tint, androidx.compose.ui.geometry.Offset(right - corner, bottom), androidx.compose.ui.geometry.Offset(right, bottom), stroke.width, StrokeCap.Round)
                drawLine(tint, androidx.compose.ui.geometry.Offset(right, bottom), androidx.compose.ui.geometry.Offset(right, bottom - corner), stroke.width, StrokeCap.Round)
                drawCircle(tint, radius = side * 0.12f, center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2), style = stroke)
            }
            InspectionGlyphKind.Camera -> {
                val body = Path().apply {
                    moveTo(inset, side * 0.34f); lineTo(side * 0.36f, side * 0.34f)
                    lineTo(side * 0.45f, side * 0.22f); lineTo(side * 0.67f, side * 0.22f)
                    lineTo(side * 0.76f, side * 0.34f); lineTo(right, side * 0.34f)
                    lineTo(right, bottom); lineTo(inset, bottom); close()
                }
                drawPath(body, tint, style = stroke)
                drawCircle(tint, radius = side * 0.18f, center = androidx.compose.ui.geometry.Offset(size.width / 2, side * 0.63f), style = stroke)
            }
            InspectionGlyphKind.Gallery -> {
                drawRoundRect(tint, androidx.compose.ui.geometry.Offset(inset, inset), androidx.compose.ui.geometry.Size(side * 0.72f, side * 0.72f), androidx.compose.ui.geometry.CornerRadius(side * 0.08f), style = stroke)
                drawCircle(tint, radius = side * 0.065f, center = androidx.compose.ui.geometry.Offset(side * 0.34f, side * 0.34f))
                val hills = Path().apply {
                    moveTo(side * 0.18f, side * 0.78f); lineTo(side * 0.43f, side * 0.52f)
                    lineTo(side * 0.57f, side * 0.66f); lineTo(side * 0.68f, side * 0.55f)
                    lineTo(side * 0.86f, side * 0.74f)
                }
                drawPath(hills, tint, style = stroke)
            }
            InspectionGlyphKind.Check -> {
                drawCircle(tint, radius = side * 0.39f, center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2), style = stroke)
                val check = Path().apply {
                    moveTo(side * 0.28f, side * 0.51f); lineTo(side * 0.44f, side * 0.66f); lineTo(side * 0.73f, side * 0.35f)
                }
                drawPath(check, tint, style = stroke)
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
    val shape = RoundedCornerShape(15.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                shape = shape,
            )
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(10.dp), shape = RoundedCornerShape(50), color = accent) {}
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
                Text(cue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Text("Selected", style = MaterialTheme.typography.labelMedium, color = accent)
        }
    }
}

@Composable
private fun LlamaAssistantPanel(context: Context, defectLabel: String?) {
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
    var message by remember {
        mutableStateOf(
            if (modelPath != null && tokenizerPath != null) "Complete saved Llama files found on this phone. Load them to generate an experimental explanation."
            else if (File(llamaDirectory, "Llama-3.2-1B-Instruct.pte").exists() || File(llamaDirectory, "tokenizer.model").exists()) "Saved Llama files are incomplete or mismatched. Select the complete downloaded model and matching tokenizer again."
            else "Select the complete ExecuTorch model and matching tokenizer. Incomplete copies are rejected before native loading."
        )
    }

    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            message = "Copying model to app storage…"
            runCatching { LlamaModelStore.copyIntoPrivateStorage(context, uri, "Llama-3.2-1B-Instruct.pte") }
                .onSuccess { modelPath = it.absolutePath; message = "Model saved on this phone." }
                .onFailure { message = "Could not copy model: ${it.message}" }
         }
    }
    val tokenizerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            message = "Copying tokenizer…"
            runCatching { LlamaModelStore.copyIntoPrivateStorage(context, uri, "tokenizer.model") }
                .onSuccess { tokenizerPath = it.absolutePath; message = "Tokenizer saved on this phone." }
                .onFailure { message = "Could not copy tokenizer: ${it.message}" }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeading("Llama recommendation", "OPTIONAL TEXT ASSISTANT · ON-DEVICE")
            Text("The checklist above is the immediate guidance. Llama receives the predicted class and checklist as text; it cannot inspect the image or assign severity. Its reply may be wrong.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(message, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !loadingModel && module == null, onClick = { modelPicker.launch(arrayOf("application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("Choose Llama model") }
            OutlinedButton(enabled = !loadingModel && module == null, onClick = { tokenizerPicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("Choose tokenizer") }
            FilledTonalButton(
                enabled = modelPath != null && tokenizerPath != null && module == null && !loadingModel,
                onClick = {
                    val mp = modelPath ?: return@FilledTonalButton
                    val tp = tokenizerPath ?: return@FilledTonalButton
                    loadingModel = true
                    message = "Verifying the complete model and tokenizer…"
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
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
                        result.onSuccess { module = it; message = "Llama loaded locally and ready." }
                            .onFailure { message = "Model was not loaded: ${it.message ?: "file verification failed"}. Choose the full original model and matching tokenizer." }
                    }
                }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp),
            ) { Text(if (loadingModel) "Verifying / loading Llama…" else "Load Llama") }
            Button(
                enabled = module != null && defectLabel != null,
                onClick = {
                    val llm = module ?: return@Button
                    val label = defectLabel ?: return@Button
                    answer = ""
                    message = "Generating locally…"
                    val approvedSteps = checklistText
                    val fullPrompt = buildLlamaPrompt(label, approvedSteps)
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            llm.resetContext()
                            val generationConfig = LlmGenerationConfig.create()
                                .seqLen(128)
                                .maxNewTokens(24)
                                .temperature(0.1f)
                                .echo(false)
                                .build()
                            llm.generate(fullPrompt, generationConfig, object : LlmCallback {
                                override fun onResult(token: String) { mainHandler.post { answer = (answer + token).take(500) } }
                                override fun onStats(statsJson: String) { mainHandler.post { message = "Local generation complete. The fixed checklist remains the guidance." } }
                                override fun onError(errorCode: Int, errorMessage: String) { mainHandler.post { answer = ""; message = "Llama could not generate a response. Use the fixed checklist above." } }
                            })
                        }.onFailure {
                            mainHandler.post {
                                answer = ""
                                message = "Llama could not generate a short reply. Use the fixed checklist above."
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp),
            ) { Text("Generate Llama recommendation") }
            val displayAnswer = cleanLlamaOutput(answer)
            if (displayAnswer.isNotBlank()) {
                if (isUnsupportedLlamaOutput(displayAnswer, checklistText)) {
                    Text("Llama's reply was too generic or unsupported. Use the fixed checklist above.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Experimental Llama wording — unverified; use the fixed checklist above:", style = MaterialTheme.typography.labelMedium)
                    Text(displayAnswer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun checklistFor(label: String): List<String> = when (label.lowercase().replace(' ', '_')) {
    "crazing" -> listOf("Retake a sharply focused close-up under even, diffuse lighting.", "Have an inspector compare crack-like surface patterns with the accepted reference.")
    "inclusion" -> listOf("Retake a focused close-up with glare reduced.", "Have an inspector check for embedded foreign material against the product specification.")
    "patches" -> listOf("Retake the surface with uniform lighting.", "Have an inspector compare the discolored or uneven area with an accepted sample.")
    "pitted_surface" -> listOf("Use a close, sharp image with light angled across the surface.", "Have an inspector verify whether small depressions exceed the allowed finish.")
    "rolled_in_scale" -> listOf("Capture a sharp view along the rolling direction.", "Have an inspector check for scale-like marks against the surface standard.")
    "scratches" -> listOf("Retake a focused close-up with glare reduced.", "Have an inspector verify the linear mark against scratch acceptance limits.")
    else -> listOf("Retake the image and run analysis first.", "Have a qualified inspector make the final decision.")
}

private fun syntheticChecklistFor(label: String): List<String> = when (label.lowercase().replace(' ', '_')) {
    "good" -> listOf("Compare the part with its drawing and accepted reference; a synthetic model result does not certify it.")
    "crack" -> listOf("Inspect the indicated area under magnification. Use an approved inspection method for critical parts; a photo cannot measure crack depth.")
    "scratch" -> listOf("Check whether the mark crosses a sealing or thread surface and compare it with the applicable drawing or specification.")
    "corrosion" -> listOf("Inspect for pitting and verify any material loss with an appropriate measurement method; a photo cannot measure pit depth.")
    "deformation" -> listOf("Check the part profile with the applicable dimensions or a suitable go/no-go gauge.")
    "unrelated object" -> listOf("Confirm the photo contains the intended fitting. Center the whole part and retake a sharp image if needed.")
    "unusable photo" -> listOf("Retake a sharp, evenly lit image with the fitting fully visible and unobstructed.")
    else -> listOf("Retake the image and have a qualified inspector review the part.")
}

internal fun buildLlamaPrompt(label: String, approvedChecklist: String): String = """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Rewrite only the checklist as one short actionable sentence. Include a concrete detail. Add no facts, image claims, severity, or decisions.
<|eot_id|><|start_header_id|>user<|end_header_id|>
$label
$approvedChecklist
<|eot_id|><|start_header_id|>assistant<|end_header_id|>""".trimIndent()

internal fun cleanLlamaOutput(text: String): String = text
    .replace(Regex("""<\|[^|]+\|>"""), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

internal fun isUnsupportedLlamaOutput(text: String, approvedChecklist: String): Boolean {
    val response = text.lowercase()
    val unsupportedClaims = listOf(
        "image itself", "i can see", "i see a", "the image shows", "computer vision",
        "segment", "segmentation", "bounding box", "mask", "outline the defect",
        "restate", "previous sentence", "revised sentence", "the image is",
    )
    if (unsupportedClaims.any(response::contains)) return true
    val words = response.split(Regex("[^a-z0-9]+" )).filter(String::isNotBlank)
    if (words.size !in 6..50) return true
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
    return listOf("inspector", "inspect", "verify", "check", "retake", "recheck", "compare", "capture", "use")
        .none { requiredCue -> response.contains(requiredCue) }
}

private fun loadPreviewBitmap(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }

    resolver.openInputStream(uri)?.use { input ->
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
    return resolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
}

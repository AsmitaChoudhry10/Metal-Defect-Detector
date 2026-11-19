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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Metal Inspection Assistant",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "On-device visual quality inspection",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Real-data research demo using NEU-DET steel-surface images. Held-out six-class accuracy was 83.7% on 270 images. The classifier always chooses a defect class: it cannot identify good or unrelated images, estimate severity, or locate a defect. Predictions and checklists are advisory and are not acceptance decisions.",
                style = MaterialTheme.typography.bodySmall,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                val bitmap = previewBitmap
                if (bitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Selected image preview will appear here")
                    }
                } else {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Selected metal surface image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Text(text = status, style = MaterialTheme.typography.bodyMedium)

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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Experimental class prediction", style = MaterialTheme.typography.titleMedium)
                        Text(result, style = MaterialTheme.typography.headlineSmall)
                        Text("NEU-DET held-out accuracy: 83.7% (270 images). This score applies only to the six dataset classes and is not validated for production or plumbing parts.", style = MaterialTheme.typography.bodySmall)
                        Text("The classifier always chooses one of six labels and has no calibrated confidence score. It cannot identify a defect-free part, reject an unrelated image, grade severity, or locate a defect. Treat every prediction as uncertain and require inspector review.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Suggested manual rechecks (not acceptance criteria)", style = MaterialTheme.typography.titleSmall)
                        checklistFor(result).forEach { item -> Text("• $item") }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Operator visual triage — selected by a person, not predicted by AI", style = MaterialTheme.typography.titleSmall)
                        Text("Choose the closest visible description. These team-defined tiers do not measure depth, strength, or product acceptance.", style = MaterialTheme.typography.bodySmall)
                        val rubric = severityRubricFor(result)
                        Text("Low: ${rubric.low}", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { saveOperatorSeverity("Low") }, modifier = Modifier.fillMaxWidth()) { Text("Record Low") }
                        Text("Medium: ${rubric.medium}", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { saveOperatorSeverity("Medium") }, modifier = Modifier.fillMaxWidth()) { Text("Record Medium") }
                        Text("High: ${rubric.high}", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { saveOperatorSeverity("High") }, modifier = Modifier.fillMaxWidth()) { Text("Record High") }
                        Text(
                            operatorSeverity?.let { "Recorded operator visual tier: $it" }
                                ?: "Severity not recorded. An operator must choose a visual tier if appropriate.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                LlamaAssistantPanel(context, result)

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    enabled = previewBitmap != null && !checkingKolektor,
                    onClick = {
                        val image = previewBitmap ?: return@Button
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (checkingKolektor) "Checking…" else "Run experimental normal/defect check")
                }
                Text(
                    "Optional KolektorSDD2-trained check. Its official test result was 82.7% defect recall and 86.7% precision on one controlled product domain. It is not validated for NEU-DET strips, plumbing fittings, or unrelated images; the score is not calibrated and must not be used to accept/reject parts.",
                    style = MaterialTheme.typography.bodySmall,
                )
                kolektorScore?.let { score ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
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

            Button(
                enabled = previewBitmap != null && !analyzing,
                onClick = {
                    val image = previewBitmap ?: return@Button
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
                        result.onSuccess {
                            val label = it.replace('_', ' ').replace('-', ' ')
                            prediction = label
                            status = "Analysis complete (on-device)."
                            val record = InspectionRecord(
                                id = 0,
                                label = label,
                                inspectedAt = System.currentTimeMillis(),
                                durationMs = SystemClock.elapsedRealtime() - startedAt,
                                severity = "unreviewed",
                            )
                            val savedId = withContext(Dispatchers.IO) {
                                InspectionHistoryStore(context).use { store -> store.add(record) }
                            }
                            currentInspectionId = savedId
                            history = withContext(Dispatchers.IO) {
                                InspectionHistoryStore(context).use { store -> store.recent() }
                            }
                        }.onFailure {
                            status = "Could not analyze image: ${it.message ?: "OpenCV/model error"}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (analyzing) "Analyzing…" else "Analyze image")
            }

            Button(
                onClick = {
                    val photo = File.createTempFile("inspection_", ".jpg", context.cacheDir)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photo,
                    )
                    cameraUri = uri
                    cameraCapture.launch(uri)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Take a photo")
            }

            Button(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose a saved image")
            }

            if (competitionHistory.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Recent inspections (saved on this phone)", style = MaterialTheme.typography.titleMedium)
                        competitionHistory.forEach { record ->
                            val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(Date(record.inspectedAt))
                            val severityNote = record.severity.takeUnless { it == "unreviewed" }
                                ?.let { " • severity $it" }.orEmpty()
                            Text("${record.label} • $time • ${record.durationMs} ms$severityNote")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Recommendations are general prompts, not verified inspection standards. The classifier was trained on flat steel samples, does not detect defect-free parts, and may misclassify other objects. A qualified inspector must make the final decision.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            )
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Optional on-device Llama recommendation (experimental)", style = MaterialTheme.typography.titleMedium)
            Text("The fixed checklist above is the inspection guidance. Llama receives only the predicted class and those checklist steps as text; it cannot inspect the image, confirm a defect, assign severity, or locate it. Its wording may be wrong.", style = MaterialTheme.typography.bodySmall)
            Text(message, style = MaterialTheme.typography.bodySmall)
            Button(enabled = !loadingModel && module == null, onClick = { modelPicker.launch(arrayOf("application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Choose Llama .pte file") }
            Button(enabled = !loadingModel && module == null, onClick = { tokenizerPicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Choose tokenizer.model") }
            Button(
                enabled = modelPath != null && tokenizerPath != null && module == null && !loadingModel,
                onClick = {
                    val mp = modelPath ?: return@Button
                    val tp = tokenizerPath ?: return@Button
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
                }, modifier = Modifier.fillMaxWidth(),
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
                }, modifier = Modifier.fillMaxWidth(),
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

package com.example.ui.whiteboard.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.PointF
import com.example.data.model.ShapeType
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.digitalink.recognition.*
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

enum class SmartModelStatus {
    IDLE,
    LOADING,
    READY,
    OFFLINE_FALLBACK
}

object MlKitManager {
    private const val TAG = "MlKitManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var shapesRecognizer: DigitalInkRecognizer? = null
    private var textRecognizer: DigitalInkRecognizer? = null
    private val ocrRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private var shapesModel: DigitalInkRecognitionModel? = null
    private var textModel: DigitalInkRecognitionModel? = null

    @Volatile
    private var isShapesModelDownloaded = false

    @Volatile
    private var isTextModelDownloaded = false

    @Volatile
    private var isInitialized = false

    private var loadingJob: Job? = null

    private val _smartModelStatus = MutableStateFlow(SmartModelStatus.IDLE)
    val smartModelStatus: StateFlow<SmartModelStatus> = _smartModelStatus.asStateFlow()

    private val _smartStatusMessage = MutableStateFlow("Preparing Smart Recognition...")
    val smartStatusMessage: StateFlow<String> = _smartStatusMessage.asStateFlow()

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        ensureSmartModelsLoaded(context)
    }

    /**
     * Loads and warms up ML Kit Digital Ink models on a background thread,
     * updating smartModelStatus so the UI can show a loader until ready.
     */
    fun ensureSmartModelsLoaded(context: Context) {
        if (_smartModelStatus.value == SmartModelStatus.READY) return
        if (loadingJob?.isActive == true) return

        _smartModelStatus.value = SmartModelStatus.LOADING
        _smartStatusMessage.value = "Loading Smart AI models..."

        loadingJob = scope.launch {
            try {
                // Ensure recognizers are created on background thread
                if (shapesRecognizer == null) {
                    val shapesModelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-shapes")
                    if (shapesModelId != null) {
                        val sm = DigitalInkRecognitionModel.builder(shapesModelId).build()
                        shapesModel = sm
                        shapesRecognizer = DigitalInkRecognition.getClient(
                            DigitalInkRecognizerOptions.builder(sm).build()
                        )
                    }
                }
                if (textRecognizer == null) {
                    val textModelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en")
                    if (textModelId != null) {
                        val tm = DigitalInkRecognitionModel.builder(textModelId).build()
                        textModel = tm
                        textRecognizer = DigitalInkRecognition.getClient(
                            DigitalInkRecognizerOptions.builder(tm).build()
                        )
                    }
                }

                val modelManager = RemoteModelManager.getInstance()
                val conditions = DownloadConditions.Builder().build()

                // 1. Check & download shapes model
                val sm = shapesModel
                if (sm != null) {
                    val shapesReady = ensureSingleModelReady(modelManager, sm, conditions, "Shapes")
                    isShapesModelDownloaded = shapesReady
                    if (shapesReady) {
                        _smartStatusMessage.value = "Warming up Shape engine..."
                        shapesRecognizer?.let { warmupRecognizer(it) }
                    }
                }

                // 2. Check & download English ink model
                val tm = textModel
                if (tm != null) {
                    val textReady = ensureSingleModelReady(modelManager, tm, conditions, "Letters & Symbols")
                    isTextModelDownloaded = textReady
                    if (textReady) {
                        _smartStatusMessage.value = "Warming up Handwriting engine..."
                        textRecognizer?.let { warmupRecognizer(it) }
                    }
                }

                // If user manually skipped to offline mode while downloading, preserve that
                if (_smartModelStatus.value == SmartModelStatus.OFFLINE_FALLBACK) {
                    return@launch
                }

                if (isShapesModelDownloaded || isTextModelDownloaded) {
                    _smartStatusMessage.value = "Smart Section Ready! Draw shapes, letters & symbols."
                    _smartModelStatus.value = SmartModelStatus.READY
                } else {
                    _smartStatusMessage.value = "Offline Smart Mode Ready"
                    _smartModelStatus.value = SmartModelStatus.OFFLINE_FALLBACK
                }
            } catch (e: Exception) {
                Log.w(TAG, "Smart model loading fallback to offline engine", e)
                _smartStatusMessage.value = "Offline Smart Mode Ready"
                _smartModelStatus.value = SmartModelStatus.OFFLINE_FALLBACK
            }
        }
    }

    /**
     * Allows user to immediately draw with offline geometric recognition if network is unavailable/slow.
     */
    fun forceOfflineReady() {
        _smartStatusMessage.value = "Offline Smart Mode Ready"
        _smartModelStatus.value = SmartModelStatus.OFFLINE_FALLBACK
    }

    private suspend fun ensureSingleModelReady(
        modelManager: RemoteModelManager,
        model: DigitalInkRecognitionModel,
        conditions: DownloadConditions,
        label: String
    ): Boolean {
        val alreadyDownloaded = withTimeoutOrNull(3000L) {
            suspendCancellableCoroutine<Boolean> { cont ->
                modelManager.isModelDownloaded(model)
                    .addOnSuccessListener { downloaded ->
                        if (cont.isActive) cont.resume(downloaded)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(false)
                    }
            }
        } ?: false

        if (alreadyDownloaded) {
            Log.d(TAG, "$label model already cached locally")
            return true
        }

        _smartStatusMessage.value = "Downloading $label recognition model..."
        return withTimeoutOrNull(18000L) {
            suspendCancellableCoroutine<Boolean> { cont ->
                modelManager.download(model, conditions)
                    .addOnSuccessListener {
                        Log.d(TAG, "$label model downloaded successfully")
                        if (cont.isActive) cont.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "$label model download failed: ${e.message}")
                        if (cont.isActive) cont.resume(false)
                    }
            }
        } ?: false
    }

    private suspend fun warmupRecognizer(client: DigitalInkRecognizer) {
        try {
            val warmupInk = Ink.builder()
                .addStroke(
                    Ink.Stroke.builder()
                        .addPoint(Ink.Point.create(10f, 10f, 100L))
                        .addPoint(Ink.Point.create(80f, 80f, 150L))
                        .build()
                )
                .build()
            withTimeoutOrNull(3500L) {
                recognizeWithClient(client, warmupInk)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Warmup skipped: ${e.message}")
        }
    }

    /**
     * Recognize strokes using ML Kit Digital Ink Recognition with strict timeouts.
     * If models are not yet downloaded or API fails, returns null immediately so the app
     * falls back to the instantaneous SmartShapeRecognizer without blocking or hanging.
     */
    suspend fun recognizeInk(points: List<PointF>): Pair<String, Float>? = withContext(Dispatchers.Default) {
        if (points.size < 3) return@withContext null

        if (!isShapesModelDownloaded && !isTextModelDownloaded) {
            return@withContext null
        }

        val inkBuilder = Ink.builder()
        val strokeBuilder = Ink.Stroke.builder()
        var timestamp = System.currentTimeMillis() - points.size * 10L
        for (p in points) {
            strokeBuilder.addPoint(Ink.Point.create(p.x, p.y, timestamp))
            timestamp += 10L
        }
        inkBuilder.addStroke(strokeBuilder.build())
        val ink = inkBuilder.build()

        // 1. Try shapes recognizer first if shapes model is ready
        val shapesClient = shapesRecognizer
        if (shapesClient != null && isShapesModelDownloaded) {
            try {
                val shapeResult = recognizeWithClient(shapesClient, ink)
                if (shapeResult != null && shapeResult.second >= 0.25f) {
                    return@withContext shapeResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Shapes recognition error", e)
            }
        }

        // 2. Try text recognizer if shapes didn't match and text model is ready
        val textClient = textRecognizer
        if (textClient != null && isTextModelDownloaded) {
            try {
                val textResult = recognizeWithClient(textClient, ink)
                if (textResult != null) {
                    return@withContext textResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Text recognition error", e)
            }
        }

        null
    }

    private suspend fun recognizeWithClient(client: DigitalInkRecognizer, ink: Ink): Pair<String, Float>? =
        withTimeoutOrNull(800L) {
            suspendCancellableCoroutine { continuation ->
                client.recognize(ink)
                    .addOnSuccessListener { result ->
                        val top = result.candidates.firstOrNull()
                        if (top != null) {
                            val score = top.score ?: 1.0f
                            if (continuation.isActive) continuation.resume(Pair(top.text, score))
                        } else {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
        }

    /**
     * Map ML Kit text tag to ShapeType if applicable
     */
    fun mapCandidateToShapeType(candidate: String): ShapeType? {
        val upper = candidate.uppercase().trim()
        return when {
            upper.contains("RECTANGLE") || upper.contains("RECT") || upper.contains("BOX") -> ShapeType.RECT
            upper.contains("SQUARE") -> ShapeType.SQUARE
            upper.contains("CIRCLE") -> ShapeType.CIRCLE
            upper.contains("ELLIPSE") || upper.contains("OVAL") -> ShapeType.OVAL
            upper.contains("RIGHT_TRIANGLE") || upper.contains("RIGHT TRIANGLE") -> ShapeType.RIGHT_TRIANGLE
            upper.contains("TRIANGLE") -> ShapeType.TRIANGLE
            upper.contains("STAR") -> ShapeType.STAR
            upper.contains("ARROW") -> ShapeType.ARROW
            upper.contains("LINE") -> ShapeType.LINE
            upper.contains("DIAMOND") || upper.contains("RHOMBUS") -> ShapeType.DIAMOND
            upper.contains("HEXAGON") -> ShapeType.HEXAGON
            upper.contains("PENTAGON") -> ShapeType.PENTAGON
            upper.contains("HEART") -> ShapeType.HEART
            upper.contains("CROSS") -> ShapeType.CROSS
            upper.contains("CLOUD") -> ShapeType.CLOUD
            upper.contains("PARALLELOGRAM") || upper.contains("PARALLEL") -> ShapeType.PARALLEL
            upper.contains("TRAPEZOID") -> ShapeType.TRAPEZOID
            upper.contains("SEMICIRCLE") -> ShapeType.SEMICIRCLE
            else -> null
        }
    }

    /**
     * Run OCR on a bitmap image using ML Kit Text Recognition.
     */
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            ocrRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "OCR Recognition error", e)
                    if (continuation.isActive) continuation.resume("")
                }
        }
    }
}

package com.example.ui.whiteboard.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.PointF
import com.example.data.model.ShapeType
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.digitalink.*
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object MlKitManager {
    private const val TAG = "MlKitManager"

    private var shapesRecognizer: DigitalInkRecognizer? = null
    private var textRecognizer: DigitalInkRecognizer? = null
    private var ocrRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var shapesModel: DigitalInkRecognitionModel? = null
    private var textModel: DigitalInkRecognitionModel? = null

    @Volatile
    private var isShapesModelDownloaded = false

    @Volatile
    private var isTextModelDownloaded = false

    @Volatile
    private var isDownloading = false

    fun initialize(context: Context) {
        val modelManager = RemoteModelManager.getInstance()

        // 1. Setup Shapes Model (zxx-Zsym-x-shapes)
        val shapesModelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-shapes")
        if (shapesModelId != null) {
            val sm = DigitalInkRecognitionModel.builder(shapesModelId).build()
            shapesModel = sm
            val options = DigitalInkRecognizerOptions.builder(sm).build()
            shapesRecognizer = DigitalInkRecognition.getClient(options)

            modelManager.isModelDownloaded(sm)
                .addOnSuccessListener { downloaded ->
                    isShapesModelDownloaded = downloaded
                    if (!downloaded) {
                        triggerModelDownload()
                    } else {
                        Log.d(TAG, "Shapes model already downloaded")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed checking shapes model status", e)
                }
        }

        // 2. Setup English Text Model (en) for letters, digits, words, symbols
        val textModelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en")
        if (textModelId != null) {
            val tm = DigitalInkRecognitionModel.builder(textModelId).build()
            textModel = tm
            val options = DigitalInkRecognizerOptions.builder(tm).build()
            textRecognizer = DigitalInkRecognition.getClient(options)

            modelManager.isModelDownloaded(tm)
                .addOnSuccessListener { downloaded ->
                    isTextModelDownloaded = downloaded
                    if (!downloaded) {
                        triggerModelDownload()
                    } else {
                        Log.d(TAG, "Text model already downloaded")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed checking text model status", e)
                }
        }
    }

    /**
     * Trigger background download of models using lenient conditions so it works on any network.
     */
    fun triggerModelDownload() {
        if (isDownloading) return
        isDownloading = true

        val modelManager = RemoteModelManager.getInstance()
        val conditions = DownloadConditions.Builder().build()

        shapesModel?.let { sm ->
            if (!isShapesModelDownloaded) {
                modelManager.download(sm, conditions)
                    .addOnSuccessListener {
                        isShapesModelDownloaded = true
                        isDownloading = false
                        Log.d(TAG, "Shapes model downloaded successfully")
                    }
                    .addOnFailureListener { e ->
                        isDownloading = false
                        Log.w(TAG, "Shapes model download failed; will use fast geometric classifier", e)
                    }
            }
        }

        textModel?.let { tm ->
            if (!isTextModelDownloaded) {
                modelManager.download(tm, conditions)
                    .addOnSuccessListener {
                        isTextModelDownloaded = true
                        isDownloading = false
                        Log.d(TAG, "Text ink model downloaded successfully")
                    }
                    .addOnFailureListener { e ->
                        isDownloading = false
                        Log.w(TAG, "Text ink model download failed; will use fast geometric classifier", e)
                    }
            }
        }
    }

    /**
     * Recognize strokes using ML Kit Digital Ink Recognition with strict timeouts.
     * If models are not yet downloaded or API fails, returns null immediately so the app
     * falls back to the instantaneous SmartShapeRecognizer without blocking or hanging.
     */
    suspend fun recognizeInk(points: List<PointF>): Pair<String, Float>? = withContext(Dispatchers.Default) {
        if (points.size < 3) return@withContext null

        // If neither model is downloaded yet, trigger background download and return null immediately.
        // This ensures ZERO lag or hang for the user!
        if (!isShapesModelDownloaded && !isTextModelDownloaded) {
            triggerModelDownload()
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

package com.example.ui.whiteboard.ml

import android.content.Context
import android.graphics.Bitmap
import com.example.data.model.PointF
import android.util.Log
import com.example.data.model.CanvasElement
import com.example.data.model.ShapeType
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.digitalink.*
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object MlKitManager {
    private const val TAG = "MlKitManager"

    private var shapesRecognizer: DigitalInkRecognizer? = null
    private var textRecognizer: DigitalInkRecognizer? = null
    private var ocrRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var isShapesModelDownloaded = false
    private var isTextModelDownloaded = false

    fun initialize(context: Context) {
        val modelManager = RemoteModelManager.getInstance()

        // 1. Setup Shapes Model (zxx-Zsym-x-shapes)
        val shapesModelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-shapes")
        if (shapesModelId != null) {
            val shapesModel = DigitalInkRecognitionModel.builder(shapesModelId).build()
            val options = DigitalInkRecognizerOptions.builder(shapesModel).build()
            shapesRecognizer = DigitalInkRecognition.getClient(options)

            modelManager.isModelDownloaded(shapesModel)
                .addOnSuccessListener { downloaded ->
                    isShapesModelDownloaded = downloaded
                    if (!downloaded) {
                        modelManager.download(shapesModel, DownloadConditions.Builder().build())
                            .addOnSuccessListener {
                                isShapesModelDownloaded = true
                                Log.d(TAG, "Shapes model downloaded successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Shapes model download failed, will fallback to geometric classifier", e)
                            }
                    }
                }
        }

        // 2. Setup English Text Model (en) for letters, digits, words, symbols
        val textModelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en")
        if (textModelId != null) {
            val textModel = DigitalInkRecognitionModel.builder(textModelId).build()
            val options = DigitalInkRecognizerOptions.builder(textModel).build()
            textRecognizer = DigitalInkRecognition.getClient(options)

            modelManager.isModelDownloaded(textModel)
                .addOnSuccessListener { downloaded ->
                    isTextModelDownloaded = downloaded
                    if (!downloaded) {
                        modelManager.download(textModel, DownloadConditions.Builder().build())
                            .addOnSuccessListener {
                                isTextModelDownloaded = true
                                Log.d(TAG, "Text ink model downloaded successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Text ink model download failed", e)
                            }
                    }
                }
        }
    }

    /**
     * Recognize strokes using ML Kit Digital Ink Recognition.
     * Returns recognized shape or text string candidate, or null if unconfident/unavailable.
     */
    suspend fun recognizeInk(points: List<PointF>): Pair<String, Float>? = withContext(Dispatchers.Default) {
        if (points.size < 3) return@withContext null

        val inkBuilder = Ink.builder()
        val strokeBuilder = Ink.Stroke.builder()
        var timestamp = System.currentTimeMillis() - points.size * 10L
        for (p in points) {
            strokeBuilder.addPoint(Ink.Point.create(p.x, p.y, timestamp))
            timestamp += 10L
        }
        inkBuilder.addStroke(strokeBuilder.build())
        val ink = inkBuilder.build()

        // 1. Try shapes recognizer first
        val shapesClient = shapesRecognizer
        if (shapesClient != null) {
            val shapeResult = recognizeWithClient(shapesClient, ink)
            if (shapeResult != null && shapeResult.second >= 0.25f) {
                return@withContext shapeResult
            }
        }

        // 2. Try text recognizer if shapes didn't match
        val textClient = textRecognizer
        if (textClient != null) {
            val textResult = recognizeWithClient(textClient, ink)
            if (textResult != null) {
                return@withContext textResult
            }
        }

        null
    }

    private suspend fun recognizeWithClient(client: DigitalInkRecognizer, ink: Ink): Pair<String, Float>? =
        suspendCancellableCoroutine { continuation ->
            client.recognize(ink)
                .addOnSuccessListener { result ->
                    val top = result.candidates.firstOrNull()
                    if (top != null) {
                        val score = top.score ?: 1.0f
                        continuation.resume(Pair(top.text, score))
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
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
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "OCR Recognition error", e)
                    continuation.resume("")
                }
        }
    }
}

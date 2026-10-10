package com.fungiradar.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector

/**
 * Requires a TFLite Task Vision ObjectDetector-compatible model with metadata,
 * normalization details, labels and supported detection outputs.
 * Raw YOLO exports often need custom decoding and are NOT plug-and-play.
 */
class MushroomDetector(context: Context) : AutoCloseable {
    private val detector: ObjectDetector? = try {
        ObjectDetector.createFromFileAndOptions(
            context, "mushroom_detector.tflite",
            ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(30).setScoreThreshold(0.1f).build()
        )
    } catch (_: Exception) { null }

    val available: Boolean get() = detector != null

    fun detect(bitmap: Bitmap, threshold: Float): List<Detection> {
        val engine = detector ?: return emptyList()
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        return engine.detect(TensorImage.fromBitmap(bitmap)).mapNotNull { result ->
            val category = result.categories.maxByOrNull { it.score } ?: return@mapNotNull null
            if (category.score < threshold) return@mapNotNull null
            val r = result.boundingBox
            Detection(RectF((r.left / w).coerceIn(0f, 1f),
                (r.top / h).coerceIn(0f, 1f),
                (r.right / w).coerceIn(0f, 1f),
                (r.bottom / h).coerceIn(0f, 1f)),
                category.label.ifBlank { "Mushroom?" }, category.score)
        }
    }
    override fun close() { detector?.close() }
}

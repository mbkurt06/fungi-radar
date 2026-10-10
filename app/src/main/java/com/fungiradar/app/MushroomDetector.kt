package com.fungiradar.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File

/** Loads compatible TFLite Task Vision object detectors from app-private storage. */
class MushroomDetector(private val context: Context) : AutoCloseable {
    private var engine: ObjectDetector? = null
    var activeModel: String? = null
        private set
    var lastError: String? = null
        private set

    fun load(file: File?): Boolean {
        close()
        lastError = null
        if (file == null) return false
        return try {
            engine = ObjectDetector.createFromFileAndOptions(
                file, ObjectDetector.ObjectDetectorOptions.builder()
                    .setMaxResults(30).setScoreThreshold(0.1f).build())
            activeModel = file.name
            true
        } catch (e: Exception) {
            lastError = e.message ?: "Incompatible TFLite detector"
            false
        }
    }

    val available: Boolean get() = engine != null

    fun detect(bitmap: Bitmap, threshold: Float): List<Detection> {
        val detector = engine ?: return emptyList()
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        return detector.detect(TensorImage.fromBitmap(bitmap)).mapNotNull { result ->
            val category = result.categories.maxByOrNull { it.score } ?: return@mapNotNull null
            if (category.score < threshold) return@mapNotNull null
            val r = result.boundingBox
            Detection(RectF((r.left / w).coerceIn(0f, 1f),
                (r.top / h).coerceIn(0f, 1f),
                (r.right / w).coerceIn(0f, 1f),
                (r.bottom / h).coerceIn(0f, 1f)),
                category.label.ifBlank { "Object" }, category.score)
        }
    }
    override fun close() {
        engine?.close()
        engine = null
        activeModel = null
    }
}

package com.fungiradar.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import kotlin.math.max
import kotlin.math.min

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
                    .setMaxResults(25)
                    .setScoreThreshold(0.08f)
                    .build()
            )
            activeModel = file.name
            true
        } catch (e: Exception) {
            lastError = e.message ?: "Incompatible TFLite detector"
            false
        }
    }

    val available: Boolean get() = engine != null

    fun detect(bitmap: Bitmap, threshold: Float, targetLabel: String?): List<Detection> {
        val detector = engine ?: return emptyList()
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val raw = detector.detect(TensorImage.fromBitmap(bitmap)).mapNotNull { result ->
            val category = result.categories.maxByOrNull { it.score } ?: return@mapNotNull null
            if (category.score < threshold) return@mapNotNull null
            val label = category.label.ifBlank { "Object" }
            if (!targetLabel.isNullOrBlank() && !label.equals(targetLabel, ignoreCase = true)) {
                return@mapNotNull null
            }
            val r = result.boundingBox
            Detection(
                RectF(
                    (r.left / w).coerceIn(0f, 1f),
                    (r.top / h).coerceIn(0f, 1f),
                    (r.right / w).coerceIn(0f, 1f),
                    (r.bottom / h).coerceIn(0f, 1f)
                ),
                label, category.score
            )
        }.sortedByDescending { it.score }

        val kept = mutableListOf<Detection>()
        for (candidate in raw) {
            if (kept.none { it.label.equals(candidate.label, true) && iou(it.rect, candidate.rect) > 0.65f }) {
                kept += candidate
                if (kept.size >= 12) break
            }
        }
        return kept
    }

    private fun iou(a: RectF, b: RectF): Float {
        val left = max(a.left, b.left)
        val top = max(a.top, b.top)
        val right = min(a.right, b.right)
        val bottom = min(a.bottom, b.bottom)
        val iw = max(0f, right - left)
        val ih = max(0f, bottom - top)
        val intersection = iw * ih
        val union = a.width() * a.height() + b.width() * b.height() - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    override fun close() {
        engine?.close()
        engine = null
        activeModel = null
    }
}

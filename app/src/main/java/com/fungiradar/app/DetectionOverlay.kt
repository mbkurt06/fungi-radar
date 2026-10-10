package com.fungiradar.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

data class Detection(val rect: RectF, val label: String, val score: Float)

class DetectionOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var detections: List<Detection> = emptyList()
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(86, 230, 126); style = Paint.Style.STROKE; strokeWidth = 6f
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 42f; typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    fun show(items: List<Detection>) { detections = items; invalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (item in detections) {
            val box = RectF(item.rect.left * width, item.rect.top * height,
                item.rect.right * width, item.rect.bottom * height)
            canvas.drawRoundRect(box, 12f, 12f, stroke)
            canvas.drawText("${item.label} ${(item.score * 100).toInt()}%",
                box.left, (box.top - 12f).coerceAtLeast(45f), text)
        }
    }
}

package com.fungiradar.app

data class ModelPreset(
    val title: String,
    val fileName: String,
    val url: String,
    val note: String
)

object ModelCatalog {
    val presets = listOf(
        ModelPreset(
            title = "SSD MobileNet V1 • fastest",
            fileName = "ssd_mobilenet_v1_coco.tflite",
            url = "https://tfhub.dev/tensorflow/lite-model/ssd_mobilenet_v1/1/metadata/1?lite-format=tflite",
            note = "COCO 80 classes • fastest baseline • no mushroom/tree class"
        ),
        ModelPreset(
            title = "EfficientDet Lite0 • balanced",
            fileName = "efficientdet_lite0_coco.tflite",
            url = "https://tfhub.dev/tensorflow/lite-model/efficientdet/lite0/detection/metadata/1?lite-format=tflite",
            note = "COCO 80 classes • better quality than SSD • mobile friendly"
        ),
        ModelPreset(
            title = "EfficientDet Lite1 • quality",
            fileName = "efficientdet_lite1_coco.tflite",
            url = "https://tfhub.dev/tensorflow/lite-model/efficientdet/lite1/detection/metadata/1?lite-format=tflite",
            note = "COCO 80 classes • medium model • slower, usually more accurate"
        ),
        ModelPreset(
            title = "EfficientDet Lite2 • highest quality",
            fileName = "efficientdet_lite2_coco.tflite",
            url = "https://tfhub.dev/tensorflow/lite-model/efficientdet/lite2/detection/metadata/1?lite-format=tflite",
            note = "COCO 80 classes • 448×448 input • slowest preset, best COCO accuracy here"
        )
    )

    fun friendlyName(fileName: String?): String {
        if (fileName.isNullOrBlank()) return "No model"
        return presets.firstOrNull { it.fileName == fileName }?.title
            ?: fileName.removeSuffix(".tflite")
    }
}

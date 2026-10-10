package com.fungiradar.app

data class ModelPreset(
    val title:String,
    val fileName:String,
    val url:String?,
    val note:String,
    val group:String
)

object ModelCatalog {
    val presets=listOf(
        ModelPreset("GENERAL • SSD MobileNet V1 • fastest","ssd_mobilenet_v1_coco.tflite","https://tfhub.dev/tensorflow/lite-model/ssd_mobilenet_v1/1/metadata/1?lite-format=tflite","COCO 80 classes • fastest baseline • no mushroom/tree class","General objects"),
        ModelPreset("GENERAL • EfficientDet Lite0 • balanced","efficientdet_lite0_coco.tflite","https://tfhub.dev/tensorflow/lite-model/efficientdet/lite0/detection/metadata/1?lite-format=tflite","COCO 80 classes • mobile friendly","General objects"),
        ModelPreset("GENERAL • EfficientDet Lite1 • quality","efficientdet_lite1_coco.tflite","https://tfhub.dev/tensorflow/lite-model/efficientdet/lite1/detection/metadata/1?lite-format=tflite","COCO 80 classes • medium model • slower, usually more accurate","General objects"),
        ModelPreset("GENERAL • EfficientDet Lite2 • highest quality","efficientdet_lite2_coco.tflite","https://tfhub.dev/tensorflow/lite-model/efficientdet/lite2/detection/metadata/1?lite-format=tflite","COCO 80 classes • 448×448 • slowest preset here","General objects"),
        ModelPreset("NATURE • YOLOv8n Open Images V7 • recommended","yolov8n-oiv7.onnx","https://huggingface.co/DearthAI/danmu-detector/resolve/main/yolov8n-oiv7.onnx?download=true","601 classes including Mushroom and Tree • direct download • ONNX","Nature"),
        ModelPreset("NATURE • YOLOv8s Open Images V7 • higher quality","yolov8s-oiv7_float32.tflite",null,"Larger 601-class model. Must currently be exported on a PC and imported.","Nature")
    )

    private val legacyNames=mapOf(
        "tensorflow_ssd_mobilenet_v1_coco_test.tflite" to "GENERAL • SSD MobileNet V1 • test",
        "tensorflow_ssd_mobilenet_v1_coco_test" to "GENERAL • SSD MobileNet V1 • test"
    )

    fun friendlyName(fileName:String?):String {
        if(fileName.isNullOrBlank()) return "No model"
        return presets.firstOrNull{it.fileName==fileName}?.title
            ?: legacyNames[fileName]
            ?: legacyNames[fileName.removeSuffix(".tflite")]
            ?: when {
                fileName.contains("yolov8n-oiv7",true)->"NATURE • YOLOv8n Open Images V7"
                fileName.contains("yolov8s-oiv7",true)->"NATURE • YOLOv8s Open Images V7"
                else->fileName.substringBeforeLast('.')
            }
    }

    fun shortName(fileName:String?):String {
        val full=friendlyName(fileName)
        return when {
            full.contains("EfficientDet Lite0")->"EfficientDet Lite0"
            full.contains("EfficientDet Lite1")->"EfficientDet Lite1"
            full.contains("EfficientDet Lite2")->"EfficientDet Lite2"
            full.contains("SSD MobileNet V1") && full.contains("test",true)->"SSD MobileNet V1 test"
            full.contains("SSD MobileNet V1")->"SSD MobileNet V1"
            full.contains("YOLOv8n",true)->"YOLOv8n Open Images V7"
            full.contains("YOLOv8s",true)->"YOLOv8s Open Images V7"
            else->full
        }
    }
}

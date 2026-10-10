package com.fungiradar.app
data class ModelPreset(val title:String,val fileName:String,val url:String?,val note:String,val group:String)
object ModelCatalog {
    val presets=listOf(
        ModelPreset("GENERAL • SSD MobileNet V1 • fastest","ssd_mobilenet_v1_coco.tflite","https://tfhub.dev/tensorflow/lite-model/ssd_mobilenet_v1/1/metadata/1?lite-format=tflite","COCO 80 classes • fastest baseline • no mushroom/tree class","General objects"),
        ModelPreset("GENERAL • EfficientDet Lite0 • balanced","efficientdet_lite0_coco.tflite","https://tfhub.dev/tensorflow/lite-model/efficientdet/lite0/detection/metadata/1?lite-format=tflite","COCO 80 classes • mobile friendly","General objects"),
        ModelPreset("GENERAL • EfficientDet Lite1 • quality","efficientdet_lite1_coco.tflite","https://tfhub.dev/tensorflow/lite-model/efficientdet/lite1/detection/metadata/1?lite-format=tflite","COCO 80 classes • medium model • slower, usually more accurate","General objects"),
        ModelPreset("GENERAL • EfficientDet Lite2 • highest quality","efficientdet_lite2_coco.tflite","https://tfhub.dev/tensorflow/lite-model/efficientdet/lite2/detection/metadata/1?lite-format=tflite","COCO 80 classes • 448×448 • slowest preset here","General objects"),
        ModelPreset("NATURE • YOLOv8n Open Images V7 • Mushroom + Tree","yolov8n-oiv7_float32.tflite",null,"601 classes including Mushroom (#347) and Tree (#553). Export on PC, then import the .tflite file.","Nature"),
        ModelPreset("NATURE • YOLOv8s Open Images V7 • higher quality","yolov8s-oiv7_float32.tflite",null,"Larger 601-class model. More accurate but heavier. Export on PC, then import.","Nature")
    )
    private val legacyNames=mapOf(
        "tensorflow_ssd_mobilenet_v1_coco_test.tflite" to "GENERAL • SSD MobileNet V1 • test",
        "tensorflow_ssd_mobilenet_v1_coco_test" to "GENERAL • SSD MobileNet V1 • test"
    )
    fun friendlyName(fileName:String?):String {
        if(fileName.isNullOrBlank()) return "No model"
        return presets.firstOrNull{it.fileName==fileName}?.title ?: legacyNames[fileName]
            ?: legacyNames[fileName.removeSuffix(".tflite")]
            ?: when {
                fileName.contains("yolov8n-oiv7",true)->"NATURE • YOLOv8n Open Images V7"
                fileName.contains("yolov8s-oiv7",true)->"NATURE • YOLOv8s Open Images V7"
                else->fileName.removeSuffix(".tflite")
            }
    }
}

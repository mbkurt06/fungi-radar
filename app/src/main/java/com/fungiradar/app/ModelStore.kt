package com.fungiradar.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Import, download and select model files locally. */
class ModelStore(private val context: Context) {
    private val dir = File(context.filesDir, "models").apply { mkdirs() }
    private val preferences = context.getSharedPreferences("models", Context.MODE_PRIVATE)

    fun list(): List<File> = dir.listFiles()?.filter { it.isFile && it.extension == "tflite" }
        ?.sortedBy { it.name } ?: emptyList()

    fun selected(): File? = list().firstOrNull { it.name == preferences.getString("selected", "") }

    fun select(file: File) {
        require(file.parentFile == dir)
        preferences.edit().putString("selected", file.name).apply()
    }

    fun import(uri: Uri): File {
        val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        } ?: "imported.tflite"
        require(displayName.endsWith(".tflite", true)) { "Choose a .tflite file" }
        val safe = displayName.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9_-]"), "_").take(60)
        val target = File(dir, "${safe}_${System.currentTimeMillis()}.tflite")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Cannot open selected file")
            validate(target)
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        return target
    }

    fun downloadTestDetector(): File {
        // Official TensorFlow Lite Task Vision demo model.
        // COCO does NOT contain a mushroom class. This preset exists only to verify
        // camera -> TFLite -> bounding-box -> alert integration on the phone.
        val target = File(dir, "tensorflow_ssd_mobilenet_v1_coco_test.tflite")
        if (target.exists() && target.length() > 1_000_000) return target

        val url = URL("https://tfhub.dev/tensorflow/lite-model/ssd_mobilenet_v1/1/metadata/1?lite-format=tflite")
        var connection: HttpURLConnection? = null
        try {
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            connection.connect()
            require(connection.responseCode in 200..299) {
                "Download failed: HTTP ${connection.responseCode}"
            }
            connection.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            validate(target)
            return target
        } catch (e: Exception) {
            target.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    private fun validate(file: File) {
        require(file.length() in 1..150_000_000) { "Invalid model file size" }
    }
}

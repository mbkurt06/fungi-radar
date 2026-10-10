package com.fungiradar.app

import android.content.Context
import android.net.Uri
import java.io.File

/** Import and select model files locally. No network or external API is used. */
class ModelStore(private val context: Context) {
    private val dir = File(context.filesDir, "models").apply { mkdirs() }
    private val preferences = context.getSharedPreferences("models", Context.MODE_PRIVATE)
    fun list(): List<File> = dir.listFiles()?.filter { it.isFile && it.extension == "tflite" }
        ?.sortedBy { it.name } ?: emptyList()
    fun selected(): File? = list().firstOrNull { it.name == preferences.getString("selected", "") }
    fun select(file: File) { require(file.parentFile == dir); preferences.edit().putString("selected", file.name).apply() }
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
            require(target.length() in 1..150_000_000) { "Invalid model file size" }
        } catch (e: Exception) { target.delete(); throw e }
        return target
    }
}

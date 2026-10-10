package com.fungiradar.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.fungiradar.app.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var ui: ActivityMainBinding
    private val executor = Executors.newSingleThreadExecutor()
    private var detector: MushroomDetector? = null
    @Volatile private var threshold = 0.5f
    private var lastAlert = 0L
    private val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else ui.status.text = "Camera permission required"
    }
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) readLocation() else ui.location.text = "GPS permission denied"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)
        detector = MushroomDetector(this)
        ui.status.text = if (detector?.available == true) "AI model loaded — scanning" else
            "Camera only: add compatible mushroom_detector.tflite to assets"
        ui.threshold.progress = 40
        ui.threshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold = (progress + 10) / 100f
                ui.thresholdLabel.text = "Confidence: ${(threshold * 100).toInt()}%"
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
        ui.gpsButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) readLocation()
            else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) startCamera()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(ui.preview.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(executor) { frame ->
                    try {
                        val engine = detector
                        if (engine?.available == true) {
                            // CameraX ImageProxy.toBitmap() returns an upright bitmap in supported versions.
                            val raw = frame.toBitmap()
                            val rotation = frame.imageInfo.rotationDegrees
                            val bitmap = if (rotation != 0) {
                                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                                android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                            } else raw
                            val found = engine.detect(bitmap, threshold)
                            runOnUiThread {
                                // Overlay assumes FILL_CENTER; camera crop alignment needs device validation.
                                ui.overlay.show(found)
                                ui.status.text = "Scanning • ${found.size} candidate(s)"
                                if (found.isNotEmpty()) alert()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread { ui.status.text = "Detection error: ${e.message}" }
                    } finally { frame.close() }
                }
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) { ui.status.text = "Camera error: ${e.message}" }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun alert() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAlert < 3500L) return
        lastAlert = now
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 160)
        val vibrator = if (Build.VERSION.SDK_INT >= 31)
            getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(160, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun readLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) return
        LocationServices.getFusedLocationProviderClient(this).lastLocation
            .addOnSuccessListener { loc ->
                ui.location.text = if (loc == null) "GPS: no recent fix" else
                    "GPS: %.5f, %.5f".format(java.util.Locale.US, loc.latitude, loc.longitude)
            }.addOnFailureListener { ui.location.text = "GPS unavailable" }
    }
    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
        detector?.close()
        tone.release()
    }
}

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
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fungiradar.app.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var ui: ActivityMainBinding
    private lateinit var store: ModelStore
    private lateinit var detector: MushroomDetector
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var threshold = 0.35f
    @Volatile private var running = false
    @Volatile private var targetLabel: String? = null
    private var lastAlert = 0L
    private val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)

    private val targetOptions = listOf(
        "All objects" to null,
        "Mushroom" to "mushroom",
        "Tree" to "tree",
        "Person" to "person",
        "Chair" to "chair",
        "Potted plant" to "potted plant",
        "Car" to "car",
        "Dog" to "dog",
        "Cat" to "cat"
    )

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else ui.status.text = "Camera permission required"
    }
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) readLocation() else ui.location.text = "GPS permission denied"
    }
    private val modelPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val file = store.import(uri)
            store.select(file)
            refreshModels(selectName = file.name)
        } catch (e: Exception) { ui.status.text = "Import failed: ${e.message}" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ViewCompat.setOnApplyWindowInsetsListener(ui.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(ui.root)

        store = ModelStore(this)
        detector = MushroomDetector(this)

        ui.presetSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            ModelCatalog.presets.map { it.title }
        )
        ui.presetSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                ui.presetInfo.text = ModelCatalog.presets[position].note
            }
        }

        ui.downloadPreset.setOnClickListener {
            val preset = ModelCatalog.presets[ui.presetSpinner.selectedItemPosition]
            ui.downloadPreset.isEnabled = false
            ui.status.text = "Downloading ${preset.title}…"
            Thread {
                try {
                    val file = store.download(preset) { percent ->
                        runOnUiThread { ui.status.text = "Downloading ${preset.title}… $percent%" }
                    }
                    store.select(file)
                    runOnUiThread {
                        ui.downloadPreset.isEnabled = true
                        refreshModels(selectName = file.name)
                        ui.status.text = "Downloaded — loading ${preset.title}"
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        ui.downloadPreset.isEnabled = true
                        ui.status.text = "Model download failed: ${e.message}"
                    }
                }
            }.start()
        }

        ui.targetSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            targetOptions.map { it.first }
        )
        ui.targetSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                targetLabel = targetOptions[position].second
                ui.overlay.show(emptyList())
            }
        }

        refreshModels()
        ui.importModel.setOnClickListener { modelPicker.launch(arrayOf("application/octet-stream", "*/*")) }

        ui.modelSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val file = store.list().getOrNull(position)
                if (file != null && file.name != detector.activeModel) {
                    running = false
                    executor.execute {
                        store.select(file)
                        val loaded = detector.load(file)
                        runOnUiThread {
                            ui.overlay.show(emptyList())
                            ui.status.text = if (loaded) "AI ready: ${file.name}" else
                                "Unsupported model: ${detector.lastError}"
                            running = loaded
                        }
                    }
                }
            }
        }

        ui.threshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold = (progress + 10) / 100f
                ui.thresholdLabel.text = "Confidence: ${(threshold * 100).toInt()}%"
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
        ui.threshold.progress = 25

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

    private fun refreshModels(selectName: String? = null) {
        val models = store.list()
        ui.modelSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            models.map { it.name }.ifEmpty { listOf("No model installed") }
        )
        val wanted = selectName ?: store.selected()?.name
        val index = models.indexOfFirst { it.name == wanted }
        if (index >= 0) ui.modelSpinner.setSelection(index)
        if (models.isEmpty()) ui.status.text = "Camera only — download a preset or import a detector"
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(ui.preview.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()
                analysis.setAnalyzer(executor) { frame ->
                    try {
                        if (running && detector.available) {
                            val raw = frame.toBitmap()
                            val rotation = frame.imageInfo.rotationDegrees
                            val bitmap = if (rotation != 0) {
                                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                                android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                            } else raw
                            val found = detector.detect(bitmap, threshold, targetLabel)
                            runOnUiThread {
                                ui.overlay.show(found)
                                val filter = targetLabel?.let { " • $it" } ?: ""
                                ui.status.text = "Scanning$filter • ${found.size} detection(s)"
                                if (found.isNotEmpty()) alert()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread { ui.status.text = "AI error: ${e.message}" }
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
        detector.close()
        tone.release()
    }
}

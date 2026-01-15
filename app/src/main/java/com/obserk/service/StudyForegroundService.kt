package com.obserk.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.obserk.ml.PenHoldingClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.sqrt

class StudyForegroundService : LifecycleService() {

    private var handLandmarker: HandLandmarker? = null
    private var penHoldingModel: PenHoldingClassifier? = null
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        private val _latestMlResult = MutableStateFlow<Boolean?>(null)
        @JvmStatic
        val latestMlResult: StateFlow<Boolean?> = _latestMlResult
    }

    override fun onCreate() {
        super.onCreate()
        setupHandLandmarker()
        try {
            penHoldingModel = PenHoldingClassifier.newInstance(this)
        } catch (e: Exception) {
            Log.e("StudyService", "Failed to load TFLite model", e)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startCameraAndAnalysis()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startCameraAndAnalysis() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA, imageCapture)
                serviceScope.launch {
                    while (true) {
                        takePhoto()
                        delay(5000) // 5秒ごとに撮影
                    }
                }
            } catch (exc: Exception) {
                Log.e("StudyService", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val outputFile = File.createTempFile("photo", ".jpg", cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    analyzePhoto(outputFile)
                    outputFile.delete()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("StudyService", "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }


    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun setupHandLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task") // assetsにモデルを配置してください
            .build()
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinHandDetectionConfidence(0.5f)
            .setNumHands(2)
            .setRunningMode(RunningMode.IMAGE)
            .build()
        handLandmarker = HandLandmarker.createFromOptions(this, options)
    }

    private fun analyzePhoto(file: File) {
        val model = penHoldingModel ?: return
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = handLandmarker?.detect(mpImage)

            val isPenHolding = if (result != null && result.landmarks().isNotEmpty()) {
                // 10個の特徴量を抽出してTFLiteに渡す
                val features = extractFeatures(result, bitmap.width, bitmap.height)
                val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 10), DataType.FLOAT32)
                inputBuffer.loadArray(features)

                val outputs = model.process(inputBuffer)
                val outputArray = outputs.outputFeature0AsTensorBuffer.floatArray
                outputArray[1] > outputArray[0] // 1: holding_pen
            } else {
                false
            }
            _latestMlResult.value = isPenHolding
            // ... (以下、通知やログの処理)
        } catch (e: Exception) {
            Log.e("StudyService", "ML analysis failed", e)
        }
    }

    private fun extractFeatures(result: HandLandmarkerResult, width: Int, height: Int): FloatArray {
        val landmarks = result.landmarks()[0]
        val xCoords = landmarks.map { it.x() }
        val yCoords = landmarks.map { it.y() }

        val hx = xCoords.average().toFloat()
        val hy = yCoords.average().toFloat()
        val hw = (xCoords.max() - xCoords.min())
        val hh = (yCoords.max() - yCoords.min())

        // ペン位置のシミュレーション（人差し指の先）
        val px = landmarks[8].x()
        val py = landmarks[8].y()
        val dist = sqrt(((hx - px) * (hx - px) + (hy - py) * (hy - py)).toDouble()).toFloat()

        return floatArrayOf(hx, hy, px, py, dist, 1.0f, hw, hh, 0.03f, 0.15f)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        cameraExecutor.shutdown()
        penHoldingModel?.close()
        _latestMlResult.value = null
    }
}

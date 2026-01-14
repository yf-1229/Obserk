package com.obserk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.obserk.R
import com.obserk.data.AppDatabase
import com.obserk.data.StudyLogEntity
import com.obserk.data.StudyLogRepository
import com.obserk.ml.PenHolding
import kotlinx.coroutines.*
import org.tensorflow.lite.support.image.TensorImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StudyForegroundService : LifecycleService() {
    private var timerJob: Job? = null
    private val CHANNEL_ID = "study_channel"
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var repository: StudyLogRepository
    private var penHoldingModel: PenHolding? = null

    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        val database = AppDatabase.getDatabase(this)
        repository = StudyLogRepository(database.studyLogDao())
        createNotificationChannel()
        setupCamera()

        // モデルを一度だけ初期化。16KB問題等のネイティブエラーを捕まえるため Throwable を使用
        try {
            penHoldingModel = PenHolding.newInstance(this)
        } catch (e: Throwable) {
            Log.e("StudyService", "Critical: Failed to load ML model. Possible 16KB alignment issue.", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 14+ では startForeground を非常に早い段階で呼ぶ必要がある
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(1, notification)
        }

        super.onStartCommand(intent, flags, startId)
        startPhotoLoop()
        return START_STICKY
    }

    private fun startPhotoLoop() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (isActive) {
                delay(60000)
                takePhoto()
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(cacheDir, "study_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    analyzePhoto(photoFile)
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e("StudyService", "Photo capture failed", exception)
                }
            }
        )
    }

    private fun analyzePhoto(file: File) {
        val model = penHoldingModel ?: return
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            val image = TensorImage.fromBitmap(bitmap)

            // モデルの出力仕様に合わせて AsTensorBuffer を使用
            val outputs = model.process(image.tensorBuffer)
            val outputBuffer = outputs.outputFeature0AsTensorBuffer
            val floatArray = outputBuffer.floatArray

            val maxIndex = floatArray.indices.maxByOrNull { floatArray[it] } ?: -1
            val confidence = floatArray.getOrNull(maxIndex) ?: 0f
            val resultText = "Class $maxIndex (${(confidence * 100).toInt()}%)"

            lifecycleScope.launch(Dispatchers.IO) {
                val currentDate = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
                repository.insert(
                    StudyLogEntity(
                        date = currentDate,
                        durationMinutes = 0,
                        imagePath = file.absolutePath,
                        mlResult = resultText
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("StudyService", "ML analysis failed", e)
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
            } catch (e: Exception) {
                Log.e("StudyService", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Obserk")
            .setContentText("Studying in progress (ML active)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Study Service", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        penHoldingModel?.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
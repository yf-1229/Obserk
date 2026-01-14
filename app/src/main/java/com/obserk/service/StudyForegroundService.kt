package com.obserk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

    private var effectiveMinutes = 0
    private var totalMinutes = 0
    private var falseConsecutiveCount = 0 // 連続 False カウンター

    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        val database = AppDatabase.getDatabase(this)
        repository = StudyLogRepository(database.studyLogDao())
        createNotificationChannel()
        setupCamera()

        try {
            penHoldingModel = PenHolding.newInstance(this)
        } catch (e: Throwable) {
            Log.e("StudyService", "Failed to load ML model", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
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
                totalMinutes++
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
            val outputs = model.process(image.tensorBuffer)
            
            val floatArray = outputs.outputFeature0AsTensorBuffer.floatArray
            val maxIndex = floatArray.indices.maxByOrNull { floatArray[it] } ?: -1
            
            val isPenHolding = (maxIndex == 0)
            if (isPenHolding) {
                effectiveMinutes++
                falseConsecutiveCount = 0 // 成功したらリセット
            } else {
                falseConsecutiveCount++ // 失敗したらカウントアップ
                
                // 5分連続で失敗した場合にハプティクスを発生
                if (falseConsecutiveCount >= 5) {
                    triggerAlertHaptic()
                    falseConsecutiveCount = 0 // 通知後はリセット（または再度5分待つ場合はそのまま）
                }
            }

            Log.d("StudyService", "Analyze: $isPenHolding, ConsecutiveFalse: $falseConsecutiveCount")

        } catch (e: Exception) {
            Log.e("StudyService", "ML analysis failed", e)
        }
    }

    private fun triggerAlertHaptic() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            // コ、コ、コ (短い3回の振動)
            val timings = longArrayOf(0, 50, 100, 50, 100, 50)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        }
    }

    override fun onDestroy() {
        saveFinalLog()
        penHoldingModel?.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    private fun saveFinalLog() {
        if (totalMinutes == 0) return
        val efficiency = (effectiveMinutes.toFloat() / totalMinutes.toFloat()) * 100f
        val currentDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        lifecycleScope.launch(Dispatchers.IO) {
            repository.insert(StudyLogEntity(startTime = System.currentTimeMillis(), endTime = System.currentTimeMillis(), durationSeconds = (totalMinutes * 60).toLong()))
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
            .setContentText("Monitoring study efficiency...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Study Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}

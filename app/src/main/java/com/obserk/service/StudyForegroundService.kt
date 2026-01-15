package com.obserk.service

import android.graphics.BitmapFactory
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.obserk.ml.PenHolding
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import kotlin.math.sqrt

// ... (クラス定義や他のメソッドは既存通り)

private var handLandmarker: HandLandmarker? = null

private fun setupHandLandmarker() {
    val baseOptions = BaseOptions.builder()
        .setModelAssetPath("hand_landmarker.task") // assetsにモデルを配置してください
        .build()
    val options = HandLandmarker.HandLandmarkerOptions.builder()
        .setBaseOptions(baseOptions)
        .setMinHandDetectionConfidence(0.5f)
        .setNumHands(2)
        .setRunningMode(com.google.mediapipe.tasks.vision.core.RunningMode.IMAGE)
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

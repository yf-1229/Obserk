package com.obserk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.obserk.ui.theme.ObserkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ObserkTheme {
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("obserk_prefs", android.content.Context.MODE_PRIVATE)
                val isCameraEnabled = prefs.getBoolean("camera_enabled", true)
                
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* Handle permission result */ }

                LaunchedEffect(isCameraEnabled) {
                    // カメラ権限のリクエスト（カメラが有効な場合のみ）
                    if (isCameraEnabled && ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        launcher.launch(Manifest.permission.CAMERA)
                    }
                }

                ObserkApp()
            }
        }
    }
}

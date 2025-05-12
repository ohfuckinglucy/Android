package com.example.visprog

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun RequestStoragePermission(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                onPermissionGranted()
            }
        }
    }
}

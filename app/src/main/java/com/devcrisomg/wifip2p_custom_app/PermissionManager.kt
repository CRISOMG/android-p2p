package com.devcrisomg.wifip2p_custom_app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat


class PermissionManager(private val context: Context) {
    private val requestPermissionLauncher =
        (context as ComponentActivity).registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach { entry ->
                Log.d("Permissions", "${entry.key}: ${if (entry.value) "granted" else "denied"}")
            }
        }

//    var privateDir: File = getExternalFilesDir(null)
    @SuppressLint("InlinedApi")
    fun requestPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        )
    }

    fun hasLocationPermissions(): Boolean {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.NEARBY_WIFI_DEVICES
        ).all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}

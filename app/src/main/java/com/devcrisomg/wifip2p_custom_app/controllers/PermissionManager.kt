package com.devcrisomg.wifip2p_custom_app.controllers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat


class PermissionManager(private val context: ComponentActivity) {
    private val permissionList = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.MANAGE_DEVICE_POLICY_INSTALL_UNKNOWN_SOURCES,
    ).apply {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }
    private fun requestManageUnknownAppSourcesPermission() {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:" + context.packageName)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("Permissions", "Error opening unknown sources settings", e)
            }
    }
    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:" + context.packageName)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("Permissions", "Error opening manage storage settings", e)
            }
        }
    }

    private val requestPermissionLauncher =
        context.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach { entry ->
                Log.d("Permissions", "${entry.key}: ${if (entry.value) "granted" else "denied"}")
            }
        }

    fun requestUriReadPermission() {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
//        setDataAndType(downloadUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(installIntent)
    }

    //    var privateDir: File = getExternalFilesDir(null)
    @SuppressLint("InlinedApi")
    fun requestPermissions() {


        if (!hasLocationPermissions()) {
//            requestManageExternalStoragePermission()
//            requestManageUnknownAppSourcesPermission()
            requestUriReadPermission()
            requestPermissionLauncher.launch(
                permissionList.toTypedArray()
            )
        }

    }

    fun hasLocationPermissions(): Boolean {
        return permissionList.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

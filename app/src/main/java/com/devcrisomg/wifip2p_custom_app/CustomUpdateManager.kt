package com.devcrisomg.wifip2p_custom_app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import java.io.File
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

class CustomUpdateManager {
        fun downloadAndInstallApk(context: Context, apkUrl: String) {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Downloading Update")
                .setDescription("Please wait...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        installApk(context)
                    }
                }
            }

            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        fun installApk(context: Context) {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        }

}
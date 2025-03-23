package com.devcrisomg.wifip2p_custom_app.controllers

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File


class CustomUpdateManager(private val context: Context) {

    private val client = OkHttpClient()
    private var downloadId: Long = -1
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private var apiUrl = "http://192.168.1.6:3000/checkForUpdates"
    /**
     * Verifica si hay una actualización disponible comparando la versión instalada con la del servidor.
     */
    fun checkForUpdate(apiUrl: String = this.apiUrl) {

//        val savedApiUrl = getApiUrl()
//        if (savedApiUrl.isNullOrEmpty()) {
//            saveApiUrl(apiUrl)
//        }

        Thread {
            try {
                Log.d("GeneralLog","checkForUpdate")

                val request = Request.Builder().url(apiUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful || response.body == null) {
                    Log.d("GeneralLog","Error en la solicitud HTTP: ${response.message}")
                    return@Thread
                }

                val jsonResponse = response.body!!.string()
                val jsonObject = JSONObject(jsonResponse)
                val latestVersion = jsonObject.getString("version")
                val apkUrl = jsonObject.getString("apk_url")

                val currentVersion = getCurrentAppVersion()

                Log.d("GeneralLog","currentVersion: $currentVersion")
                Log.d("GeneralLog","latestVersion: $latestVersion")
                if (isNewVersionAvailable(currentVersion, latestVersion)) {
                    Log.d("GeneralLog","Nueva versión disponible: $latestVersion. Descargando...")
                    downloadAndInstallApk(apkUrl)
                } else {
                    Log.d("GeneralLog","No hay actualizaciones disponibles.")
                }

            } catch (e: Exception) {
                Log.e("GeneralLog","checkForUpdate: ${e.message}.")
            }
        }.start()
    }
    private fun getApiUrl(): String? {
        val sharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("apiUrl", null)
    }
    private fun saveApiUrl(apiUrl: String) {
        val sharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("apiUrl", apiUrl)
            apply()
        }
    }
    fun showApiUrlDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Enter API URL")

        val input = EditText(context)
        input.hint = "https://example.com/update.json"
        input.inputType = InputType.TYPE_TEXT_VARIATION_URI

        val savedApiUrl = getApiUrl()
        if (!savedApiUrl.isNullOrEmpty()) {
            input.setText(savedApiUrl)
        }

        builder.setView(input)

        builder.setPositiveButton("Check") { _, _ ->
            val apiUrl = input.text.toString()
            Log.d("GeneralLog", apiUrl)
            if (apiUrl.isNotEmpty()) {
                saveApiUrl(apiUrl)
                checkForUpdate(apiUrl)
            } else {
                Toast.makeText(context, "URL cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun getDownloadUri(context: Context, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }
    /**
     * Descarga y luego instala la nueva versión del APK.
     */
    private fun downloadAndInstallApk(apkUrl: String) {
        val privateDir: File? = context.getExternalFilesDir(null)
        Log.d("GeneralLog", "privateDir $privateDir")

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Downloading Update Wifi P2P APP")
            .setDescription("Please wait...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//            .setDestinationUri()
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")
//            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "update.apk")

        downloadId = downloadManager.enqueue(request)


//        Handler(Looper.getMainLooper()).postDelayed({
//            if (isDownloadSuccessful(downloadManager, downloadId)) {
//                Log.d("GeneralLog", "Download completed, installing APK manually.")
//                installApk(downloadId, downloadManager)
//            } else {
//                Log.d("GeneralLog", "Download failed or still in progress.")
//            }
//        }, 5000)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("GeneralLog", "onReceive")

                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    if (isDownloadSuccessful(downloadManager, id)) {
                        Log.d("GeneralLog", "Download complete, installing APK.")
                        installApk(id, downloadManager)
                    } else {
                        Log.d("GeneralLog", "Error en la descarga del APK.")
                    }
                    context.unregisterReceiver(this)
                }
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        }
        context.registerReceiver(
            receiver,
            intentFilter,
            Context.RECEIVER_EXPORTED
        )
    }

    @SuppressLint("Range")
    fun getCurrDownloadManaged(): Uri? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            Log.d("GeneralLog", "Download status: $status")
        }
        cursor.close()

        return downloadManager.getUriForDownloadedFile(downloadId)
    }
    /**
     * Instala el APK descargado.
     */
    @SuppressLint("Range")
    private fun installApk(downloadId: Long, downloadManager: DownloadManager) {
        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                cursor.close()

                if (uriString != null) {
                    val apkFile = Uri.parse(uriString)
                    val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", File(apkFile.path))

                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(installIntent)
                }
            } else {
                cursor.close()
                Log.e("GeneralLog", "No se pudo obtener la URI de descarga.")
            }
        } catch (e: Exception) {
            Log.e("GeneralLog", "installApk: ${e.message}.")
        }
    }

    /**
     * Verifica si la descarga se completó correctamente.
     */
    private fun isDownloadSuccessful(downloadManager: DownloadManager, downloadId: Long): Boolean {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(columnIndex)
            cursor.close()
            return status == DownloadManager.STATUS_SUCCESSFUL
        }
        cursor.close()
        return false
    }


    /**
     * Obtiene la versión actual de la aplicación instalada.
     */
    private fun getCurrentAppVersion(): String {
         try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
             return packageInfo.versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
             return "0.0.0"
        }
    }

    /**
     * Compara si la versión disponible en el servidor es más nueva que la instalada.
     */
    private fun isNewVersionAvailable(currentVersion: String, latestVersion: String): Boolean {
        return compareVersionStrings(currentVersion, latestVersion) < 0
    }

    /**
     * Compara dos versiones en formato "x.y.z"
     * @return -1 si v1 < v2, 1 si v1 > v2, 0 si son iguales
     */
    private fun compareVersionStrings(v1: String, v2: String): Int {
        val v1Parts = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = v2.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(v1Parts.size, v2Parts.size)) {
            val part1 = v1Parts.getOrElse(i) { 0 }
            val part2 = v2Parts.getOrElse(i) { 0 }

            if (part1 != part2) return part1.compareTo(part2)
        }
        return 0
    }
}

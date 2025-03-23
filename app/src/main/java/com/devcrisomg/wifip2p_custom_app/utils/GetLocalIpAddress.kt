package com.devcrisomg.wifip2p_custom_app.utils
import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.UnknownHostException

fun getLocalIpAddress(context: Context): String? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiInfo = wifiManager.connectionInfo
    val ipAddress = wifiInfo.ipAddress

    return try {
        // Convert the integer IP to a readable format
        val ipBytes = byteArrayOf(
            (ipAddress and 0xFF).toByte(),
            (ipAddress shr 8 and 0xFF).toByte(),
            (ipAddress shr 16 and 0xFF).toByte(),
            (ipAddress shr 24 and 0xFF).toByte()
        )
        InetAddress.getByAddress(ipBytes).hostAddress
    } catch (e: UnknownHostException) {
        e.printStackTrace()
        null
    }
}
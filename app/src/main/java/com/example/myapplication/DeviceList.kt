package com.example.myapplication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun DeviceList(
    devices: MutableMap<String, ServiceInfo>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(devices.entries.toList()) { entry ->
            val deviceName = entry.key
            val serviceInfo = entry.value
            DeviceItem(serviceInfo)
        }
    }
}

@Composable
fun DeviceItem(
    serviceInfo: ServiceInfo,
    modifier: Modifier = Modifier
) {
    val socketManager = SocketManagerProvider.current
    val wifiP2PManager = WifiP2PManagerProvider.current
    val ip = serviceInfo.records?.get("ip") as? String
    val g_ip = serviceInfo.records?.get("g_ip") as? String
    val isConnected = ip?.let { socketManager.connectedSockets[it] } ?: false
    Column(
        modifier = modifier
            .padding(8.dp)
    ) {
        Text(text = "Name: ${serviceInfo.device.deviceName}")
        Text(text = "Address: ${serviceInfo.device.deviceAddress}")
        Text(text = "LAN IP: $ip")
        Text(text = "GROUP IP: $g_ip")
        Text(text = "Connected: $isConnected")
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isConnected) {
                CustomButton(
                    onClick = {
                        socketManager.closeSockets()
                    }, text = "Close"
                )
            }else {
                CustomButton(
                    onClick = {
                        ip?.let { socketManager.initClientSocket(it) }
                    },
                    modifier = Modifier.padding(end = 8.dp),
                    text = "Socket",
                    enabled = ip != null
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CustomButton(
                onClick = { wifiP2PManager.connectToDevice(serviceInfo.device, 15) },
                text = "Invite (15)"
            )
            Spacer(modifier = Modifier.width(16.dp))
            CustomButton(
                onClick = { wifiP2PManager.connectToDevice(serviceInfo.device, 0) },
                text = "Invite (0)"
            )
        }
    }
}

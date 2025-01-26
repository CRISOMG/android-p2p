package com.example.myapplication

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.SocketManager.SocketManagerProvider


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

    Column(
        modifier = modifier
            .padding(8.dp)
    ) {
        Text(text = "Name: ${serviceInfo.device.deviceName}")
        Text(text = "Address: ${serviceInfo.device.deviceAddress}")
        Text(text = "IP: ${serviceInfo.records["ip"]}")

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { socketManager.initClientSocket(serviceInfo.records["ip"] as String) },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(text = "Connect Socket")
            }
            Button(
                onClick = { socketManager.currSocketClient?.close() }
            ) {
                Text(text = "Close Socket")
            }
        }
    }
}

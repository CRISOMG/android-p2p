package com.example.myapplication

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp

@Composable
fun DeviceList(devices: List<WifiP2pDevice>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(devices) { device ->
            DeviceItem(device)
        }
    }
}

@Composable
fun DeviceItem(device: WifiP2pDevice, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(8.dp)) {
        Text(text = "Name: ${device.deviceName}")
        Text(text = "Address: ${device.deviceAddress}")
    }
}

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

@Composable
fun DeviceList(
    devices: List<WifiP2pDevice>,
    onDeviceClick: (WifiP2pDevice, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(devices) { device ->
            DeviceItem(device, onClick = onDeviceClick )
        }
    }
}

@Composable
fun DeviceItem(
    device: WifiP2pDevice,
    onClick: (WifiP2pDevice, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(8.dp)
    ) {
        Text(text = "Name: ${device.deviceName}")
        Text(text = "Address: ${device.deviceAddress}")

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { onClick(device, 15) },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(text = "Set GO (15)")
            }
            Button(
                onClick = { onClick(device, 0) }
            ) {
                Text(text = "Set GO (0)")
            }
        }
    }
}

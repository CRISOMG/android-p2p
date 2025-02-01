package com.devcrisomg.wifip2p_custom_app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    val context = LocalContext.current
    val myMacAddress = getMacAddress(context)
    val socketManager = SocketManagerProvider.current
    val wifiP2PManager = WifiP2PManagerProvider.current
    val ip = serviceInfo.records?.get("ip") as? String
    val g_ip = serviceInfo.records?.get("g_ip") as? String
    val used_ip = ip != null || g_ip != null
    val macAddress = serviceInfo.device.deviceAddress

    val isConnected =
        ip?.let { socketManager.connectedSockets[it] }
            ?: g_ip?.let { socketManager.connectedSockets[it] }
            ?: false


    Column(
        modifier = modifier
            .padding(8.dp)
    ) {
        Text(text = "Name: ${serviceInfo.device.deviceName}")
        Text(text = "Address: $macAddress")
        Text(text = "LAN IP: $ip")
        Text(text = "GROUP OWNER IP: $g_ip")
        Text(text = "MY GROUP IP: ${socketManager.socketIp.value}")
        Text(text = "GROUP STATUS: ${serviceInfo.group_status.value?.let {
            wifiP2PManager.getDeviceStatusDescription(
                it
            )
        }}")
        Text(text = "Server Socket: $isConnected")
//        Text(text = "Client Socket: ${
//            socketManager.connectedClients.find { client ->
////                socketManager.getMacAddressFromSocket(client).equals(serviceInfo.device.deviceAddress)
//                false
//            }
//        }"
//        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.padding(end = 8.dp)) {
                if (isConnected) {
                    CustomButton(
                        onClick = {
                            socketManager.closeSockets()
                        }, text = "Close"
                    )
                } else {
                    CustomButton(
                        onClick = {
                            if (myMacAddress != null) {
                                (ip)?.let { socketManager.initClientSocket(it,myMacAddress) }
                                    ?: g_ip?.let { socketManager.initClientSocket(it,myMacAddress) }
                            }
                        },
                        text = "Connect",
                        enabled = used_ip
                    )
                }
            }
            CustomButton(
                onClick = { wifiP2PManager.connectToDevice(serviceInfo.device, 15) },
                text = "Invite to Wifi Group"
            )
        }
    }
}

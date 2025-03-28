package com.devcrisomg.wifip2p_custom_app.components

import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devcrisomg.wifip2p_custom_app.controllers.SocketManagerProvider
import com.devcrisomg.wifip2p_custom_app.controllers.WifiP2PManagerProvider
import com.devcrisomg.wifip2p_custom_app.controllers.getMacAddress
import java.net.Socket
import com.devcrisomg.wifip2p_custom_app.utils.GenericSharedFlowEventBus
import androidx.lifecycle.viewModelScope
//import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.devcrisomg.wifip2p_custom_app.MainActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

//            _resolvedDevices.value = devicesMap.values.toList()
//            Handler(Looper.getMainLooper()).post {
//                _resolvedDevices.value = devicesMap.values.toList()
//            }

data class DeviceInfoModel(
    var ip: String? = null,
    var ip_list: MutableList<String?>? = null,
    var name: String? = null,
    var ip_p2p: String? = null,
    var ip_lan: String? = null,
    var records: MutableMap<String, Any?>? = null,
    var device: WifiP2pDevice? = null,
    var socket: Socket? = null,
    var group_status: MutableState<Int?> = mutableStateOf<Int?>(null)
)

@Singleton
class DeviceEventBus @Inject constructor(): GenericSharedFlowEventBus<DeviceInfoModel>()

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private var eventBus: DeviceEventBus
) : ViewModel() {
    private val _devicesMap = mutableMapOf<String, DeviceInfoModel>()

    val resolvedDevices: StateFlow<List<DeviceInfoModel>> = eventBus.events
        .map { event ->
            val key = event.name ?: "unknown_${System.currentTimeMillis()}"
//            _devicesMap[event.name ?: "unknown_${System.currentTimeMillis()}"] = event
            _devicesMap[key] = _devicesMap[key]?.copy(
                ip = event.ip ?: _devicesMap[key]?.ip,
                ip_lan = event.ip_lan ?: _devicesMap[key]?.ip_lan
            ) ?: event
            _devicesMap.values.toList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Cancela después de 5s sin suscriptores
            initialValue = emptyList()
        )

    fun clearDevices() {
        _devicesMap.clear()
    }
}

@Composable
fun DeviceList(
    modifier: Modifier = Modifier,
) {
    val context  = ( LocalContext.current ) as MainActivity
    val viewModel: DeviceViewModel = hiltViewModel(context)
    Log.d("DeviceViewModel", " DeviceList DeviceViewModel instantiated ${viewModel.hashCode()}")
    val devices by viewModel.resolvedDevices.collectAsState()
    Log.d("GeneralLog", "DeviceList composed.")
    LazyColumn(modifier = modifier) {
        items(devices) { device ->
            DeviceItem(device)
        }
    }
}



@Composable
fun DeviceItem(
    deviceInfo: DeviceInfoModel,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val myMacAddress = getMacAddress(context)
    val socketManager = SocketManagerProvider.current
    val wifiP2PManager = WifiP2PManagerProvider.current
    val ip = deviceInfo.ip
    val g_ip = deviceInfo.ip_p2p
    val lan_ip = deviceInfo.ip_lan
    val used_ip = ip != null || lan_ip != null || g_ip != null
    val macAddress = deviceInfo.device?.deviceAddress

    val isConnected =
        ip?.let { socketManager.connectedSockets[it] }
            ?: g_ip?.let { socketManager.connectedSockets[it] }
            ?: false


    Column(
        modifier = modifier
            .padding(8.dp)
    ) {
        Text(text = "Name: ${deviceInfo.name}")
        Text(text = "IP: $ip")
        Text(text = "IP P2P: $g_ip")
        Text(text = "IP LAN: $lan_ip")
        Text(text = "IP List: ${deviceInfo.ip_list?.filter { it?.contains("192.168.") ?: false }}")
//        Text(text = "MY GROUP IP: ${socketManager.socketIp.value}")
//        Text(text = "GROUP STATUS: ${deviceInfo.group_status.value?.let {
//            wifiP2PManager.getDeviceStatusDescription(
//                it
//            )
//        }}")
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
                onClick = {
                    Log.d("WiFiP2P", "Invitation request to ${deviceInfo.device?.deviceName}.")
                    deviceInfo.device?.let { wifiP2PManager.Invite(it, 15) }
                },
                text = "Invite to Wifi Group"
            )
        }
    }
}

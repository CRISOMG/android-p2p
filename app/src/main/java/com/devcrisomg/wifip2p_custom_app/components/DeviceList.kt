package com.devcrisomg.wifip2p_custom_app.components

import android.net.wifi.p2p.WifiP2pDevice
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devcrisomg.wifip2p_custom_app.MainActivity
import com.devcrisomg.wifip2p_custom_app.controllers.SocketManagerProvider
import com.devcrisomg.wifip2p_custom_app.controllers.WifiP2PManagerProvider
import com.devcrisomg.wifip2p_custom_app.controllers.getMacAddress
import com.devcrisomg.wifip2p_custom_app.utils.GenericEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.Socket
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel

//            _resolvedDevices.value = devicesMap.values.toList()
//            Handler(Looper.getMainLooper()).post {
//                _resolvedDevices.value = devicesMap.values.toList()
//            }

data class DeviceInfoModel(
    var ip: String? = null,
    var name: String? = null,
    var ip_p2p: String? = null,
    var ip_lan: String? = null,
    var records: MutableMap<String, Any?>? = null,
    var device: WifiP2pDevice? = null,
    var socket: Socket? = null,
    var group_status: MutableState<Int?> = mutableStateOf<Int?>(null)
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val eventBus: GenericEventBus<DeviceInfoModel>
) : ViewModel() {
    private val _resolvedDevices = MutableLiveData<List<DeviceInfoModel>>()
    val resolvedDevices: LiveData<List<DeviceInfoModel>> get() = _resolvedDevices
    private val devicesMap = mutableMapOf<String, DeviceInfoModel>()
    init {
        eventBus.subscribe { event ->
            Log.d("GeneralLog", "DeviceViewModel ${event.name}")
            devicesMap[event.name ?: "desconocido"] = event
            _resolvedDevices.postValue(devicesMap.values.toList())

        }
    }
}

@Composable
fun DeviceList(
    modifier: Modifier = Modifier
) {
    val context  = ( LocalContext.current ) as MainActivity
    val viewModel: DeviceViewModel = hiltViewModel()
    val resolvedDevices by viewModel.resolvedDevices.observeAsState(emptyList())
    Log.d("GeneralLog", "DeviceList composed.")


    LazyColumn(modifier = modifier) {
        items(resolvedDevices) { devices ->
            DeviceItem(devices)
        }
    }
}



@Composable
fun DeviceItem(
    serviceInfo: DeviceInfoModel,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val myMacAddress = getMacAddress(context)
    val socketManager = SocketManagerProvider.current
    val wifiP2PManager = WifiP2PManagerProvider.current
    val ip = serviceInfo.ip
    val g_ip = serviceInfo.ip_p2p
    val lan_ip = serviceInfo.ip_lan
    val used_ip = ip != null || lan_ip != null || g_ip != null
    val macAddress = serviceInfo.device?.deviceAddress

    val isConnected =
        ip?.let { socketManager.connectedSockets[it] }
            ?: g_ip?.let { socketManager.connectedSockets[it] }
            ?: false


    Column(
        modifier = modifier
            .padding(8.dp)
    ) {
        Text(text = "Name: ${serviceInfo.name}")
        Text(text = "IP: $ip")
        Text(text = "IP P2P: $g_ip")
        Text(text = "IP LAN: $lan_ip")
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
                onClick = { serviceInfo.device?.let { wifiP2PManager.connectToDevice(it, 15) } },
                text = "Invite to Wifi Group"
            )
        }
    }
}

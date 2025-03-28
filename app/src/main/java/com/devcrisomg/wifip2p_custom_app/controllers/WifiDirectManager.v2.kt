package com.devcrisomg.wifip2p_custom_app.controllers

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
//import com.devcrisomg.wifip2p_custom_app.components.DeviceDiscoveredEvent
import com.devcrisomg.wifip2p_custom_app.components.DeviceInfoModel
import com.devcrisomg.wifip2p_custom_app.utils.GenericSharedFlowEventBus
import com.devcrisomg.wifip2p_custom_app.utils.getLocalIpAddress
import java.net.Inet4Address
import java.net.NetworkInterface

@SuppressLint("HardwareIds")
fun getMacAddress(context: Context): String? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiInfo = wifiManager.connectionInfo
    return wifiInfo.macAddress
}

//interface EventBus {
//    fun publish(event: DeviceDiscoveredEvent)
//    fun subscribe(listener: (DeviceDiscoveredEvent) -> Unit)
//}


val WifiP2PManagerProvider =
    compositionLocalOf<WifiDirectManagerV2> { error("No WifiDirectManagerV2 provided") }

@Composable
fun ProvideWifiDirectManager(wifiP2PManager: WifiDirectManagerV2, content: @Composable () -> Unit) {
    CompositionLocalProvider(WifiP2PManagerProvider provides wifiP2PManager) {
        content()
    }
}


class WifiDirectManagerV2(
    private val context: Context,
    private val wifiP2pManager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,

//    private val permissionManager: PermissionManager,
) {
    val discoveredDevices = mutableStateListOf<WifiP2pDevice>()
    val discoveredServices = mutableStateMapOf<String, DeviceInfoModel>()
    val onDeviceResolved: GenericSharedFlowEventBus<DeviceInfoModel> = GenericSharedFlowEventBus()

    val connectionInfo = mutableStateOf<WifiP2pInfo?>(null)
    val currGroupState = mutableStateOf<WifiP2pGroup?>(null)
    fun handleOnActionListenerFailure(reason: Int) {
        val toast = { ms: String ->
            Toast.makeText(context, ms, Toast.LENGTH_SHORT).show()
        }
        var message: String = "Error no manejado en handleOnActionListenerFailure"
        when (reason) {
            WifiP2pManager.BUSY -> message ="El dispositivo está ocupado."
            WifiP2pManager.ERROR -> message ="Ocurrió un error inesperado. ${reason}"
            WifiP2pManager.P2P_UNSUPPORTED -> message =  "Wi-Fi Direct no está soportado en este dispositivo."

        }
        toast(message)
        Log.e("WiFiP2P", "El dispositivo está ocupado.")
    }

    fun getDeviceStatusDescription(status: Int): String {
//        public static final int CONNECTED   = 0;
//        public static final int INVITED     = 1;
//        public static final int FAILED      = 2;
//        public static final int AVAILABLE   = 3;
//        public static final int UNAVAILABLE = 4;
        return when (status) {
            0 -> "CONNECTED"
            1 -> "INVITED"
            2 -> "FAILED"
            3 -> "AVAILABLE"
            4 -> "UNAVAILABLE"
            else -> "unknown status"
        }
    }

    fun disconnect() {
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WiFiP2P", "Successfully disconnected from the group.")
            }

            override fun onFailure(reason: Int) {
                Log.e("WiFiP2P", "Failed to disconnect: $reason")
                handleOnActionListenerFailure(reason)
            }
        })
    }

    fun removePersistentGroups() {
        return;
        try {
            val method = WifiP2pManager::class.java.getMethod(
                "deletePersistentGroup",
                WifiP2pManager.Channel::class.java,
                Int::class.javaPrimitiveType,
                WifiP2pManager.ActionListener::class.java
            )

            for (groupId in 0..31) { // IDs usually range from 0 to 31
                method.invoke(wifiP2pManager,
                    channel,
                    groupId,
                    object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d("WiFiDirect", "Removed group with ID: $groupId")
                        }

                        override fun onFailure(reason: Int) {
                            Log.e("WiFiDirect", "Failed to remove group with ID: $groupId. Reason:")
                            handleOnActionListenerFailure(reason)
                        }
                    })
            }
        } catch (e: Exception) {
            Log.e("WiFiDirect", "Error removing persistent groups: ${e.message}")
        }
    }


    @SuppressLint("MissingPermission")
    fun createGroup() {
//        if (permissionManager.hasLocationPermissions()) {
            if (true) {
            wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("WiFiP2P", "New group created successfully.")
                }

                override fun onFailure(reason: Int) {
                    Log.e("WiFiP2P", "Failed to create group:")
                    handleOnActionListenerFailure(reason)
                }
            })
        } else {
            Log.e("WiFiP2P", "Failed to create group: not permissions enough")
//            permissionManager.requestPermissions()
        }

    }

    fun cancelConnectionToDevice() {
        wifiP2pManager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Conexión pendiente cancelada con éxito
                Log.d("WifiP2P", "Conexión cancelada con éxito.")
            }

            override fun onFailure(reason: Int) {
                Log.e("WifiP2P", "Error al cancelar la conexión:")
                handleOnActionListenerFailure(reason)
            }
        })

    }

    @SuppressLint("MissingPermission")
    fun Invite(device: WifiP2pDevice, groupOwnerIntent: Int = 15) {
        Log.d("WiFiP2P", "Invitation request to ${device.deviceName}.")

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC // Use Push Button Configuration (PBC)
            this.groupOwnerIntent = groupOwnerIntent
        }

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WiFiP2P", "Connection request to ${device.deviceName} initiated successfully.")
            }

            override fun onFailure(reason: Int) {
                Log.e("WiFiP2P", "P2P Connection request to ${device.deviceName} failed:")
                handleOnActionListenerFailure(reason)
            }
        })
    }


    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WiFiP2P", "Descubrimiento de peers iniciado correctamente")
            }

            override fun onFailure(reason: Int) {
                Log.e("WiFiP2P", "Error discoverPeers():")
                handleOnActionListenerFailure(reason)
            }
        })
    }

    private val handler = Handler(Looper.getMainLooper())
    private val discoveryRunnable = object : Runnable {
        override fun run() {
            discoverPeers()
            handler.postDelayed(this, 10000) // Ejecutar cada 2 segundos
        }
    }

    fun startDiscoveryLoop() {
        handler.post(discoveryRunnable)
    }

    fun stopDiscoveryLoop() {
        handler.removeCallbacks(discoveryRunnable)
    }

    @SuppressLint("MissingPermission", "NewApi")
    fun getConnectedDevices() {
        Log.d("WiFiP2P", "discoveredServices: ${
            discoveredServices.keys.map { key ->
                val service: DeviceInfoModel? = discoveredServices[key]
                mapOf(
                    "r" to service?.records,
                    "d" to service?.device?.deviceName,
                    "status" to service?.device?.status
                )
            }
        }")
//        permissionManager.hasLocationPermissions()
        wifiP2pManager.requestPeers(channel) { peers ->
            val pt = "10-0050F204-5"
//            1-0050F200-0
//            val targetDevices = peers.deviceList.filter { d -> d.primaryDeviceType == pt }
            val targetDevices = peers.deviceList
            val devicesString = targetDevices.joinToString {
                "${it.deviceName} (${
                    getDeviceStatusDescription(it.status)
                })"
            }
            Log.d("WiFiP2P", "[getConnectedDevices] Discovered Devices:\n$devicesString")
            targetDevices.forEach { device ->
                if (device?.deviceName?.isNotEmpty() == true) {

//                    val ip =
                    val newServiceInfo = DeviceInfoModel(
                        name = device.deviceName,
//                        ip = device.ipAddress.toString(),
//                        ip_p2p = device.ipAddress.toString(),
                        records = discoveredServices[device.deviceName]?.records,
                        device = device,
                        group_status = mutableStateOf(device.status)
                    )
                    discoveredServices[device.deviceName] = newServiceInfo

                    onDeviceResolved.publish(newServiceInfo)
                }
            }
        }

        wifiP2pManager.requestGroupInfo(channel) { group ->
            currGroupState.value = group
            if (group == null) {
                Log.d("WiFiP2P", "No group information available.")
                return@requestGroupInfo
            }

            val groupOwner = group.owner
            val groupPassword = group.passphrase
            val groupOwnerAddress = group.owner.deviceAddress
            val groupMembers =
                group.clientList.joinToString {
                    "${it.deviceName} ${it.deviceAddress}"
                }
            wifiP2pManager.requestConnectionInfo(channel) { info ->
                connectionInfo.value = info

                if (group.owner.deviceName?.isNotEmpty() == true) {
                    val devServ = discoveredServices.getOrPut(group.owner.deviceName) {
                        DeviceInfoModel(device = group.owner)
                    }
                    devServ.records = devServ.records?.apply {
                        this["g_ip"] = info.groupOwnerAddress?.hostAddress
                    } ?: mutableMapOf("g_ip" to info.groupOwnerAddress?.hostAddress)

                    onDeviceResolved.publish(devServ)

                }
            }
//            Log.d("WiFiP2P", "Group id: ${group.networkId}")
//            Log.d("WiFiP2P", "Group Password: $groupPassword")
//            Log.d("WiFiP2P", "Group Owner: ${groupOwner.deviceName} ($groupOwnerAddress)")
//            Log.d("WiFiP2P", "Group Members: $groupMembers")
        }
    }

    // BroadcastReceiver for peers
    private val peerChangeReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val intentKey = intent?.action?.split(".")?.last()
            Log.d("WiFiP2P", "[peerChangeReceiver] triggered by $intentKey")
            wifiP2pManager.requestGroupInfo(channel) { group ->
                currGroupState.value = group
            }
            wifiP2pManager.requestConnectionInfo(channel) { info ->
                connectionInfo.value = info
            }
            if (intentKey == "PEERS_CHANGED") {
                getConnectedDevices()
            }
        }
    }

    // BroadcastReceiver for services
    private val serviceChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            wifiP2pManager.requestConnectionInfo(channel) { info ->
                Log.d("WiFiP2P", "[serviceChangeReceiver]: $info")
                connectionInfo.value = info
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun advertiseService(tag: String  = "MyCustomTag") {
        val ip = getLocalIpAddress(context)

        wifiP2pManager.addLocalService(channel,
            WifiP2pDnsSdServiceInfo.newInstance(
                "MyOMGAppService", "_myomgapp._tcp", mapOf(
                    "tag" to tag,
                    "appVersion" to "1.0",
                    "ip" to ip,
                    "g_ip" to SocketManager.socketIp.value
                )
            ),
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("WiFiP2P", "Service advertised successfully.")
                }

                override fun onFailure(reason: Int) {
                    Log.e("WiFiP2P", "Failed to advertise service: $reason")
                }
            })
    }

    fun stopAdvertisingService() {
        wifiP2pManager.clearLocalServices(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WiFiP2P", "Local services cleared.")
            }

            override fun onFailure(reason: Int) {
                Log.e("WiFiP2P", "Failed to clear local services: ")
                handleOnActionListenerFailure(reason)
            }
        })
    }
    fun restartAdvertidingService() {
        Log.d("WiFiP2P", "SK ip ${SocketManager.socketIp.value}")

        stopAdvertisingService()
        advertiseService()
    }

    @SuppressLint("MissingPermission", "NewApi")
    fun discoverServices() {
        Log.d("WiFiP2P", "[discoverServices]")
        wifiP2pManager.setDnsSdResponseListeners(channel,
            { instanceName, registrationType, device ->
                Log.d("WiFiP2P", "[discoverServices] setDnsSdResponseListeners response")
                    Log.d(
                        "WiFiP2P",
                        "Service discovered: $instanceName on device: ${device.deviceName}"
                    )
//                    discoveredDevices.clear() // Add device to your list
//                    discoveredDevices.addAll(listOf(device)) // Add device to your list
            },
            { fullDomainName, txtRecordMap, srcDevice ->
                Log.d("WiFiP2P", "Service TXT records: $txtRecordMap")

//                txtRecordMap.apply {
//                    if (this["ip"].equals("0.0.0.0")) {
//                        this["ip"] = null
//                    }
//                }.toMutableMap()
                val ip = srcDevice.ipAddress.toString().replace("/","")
                val serviceInfo = DeviceInfoModel(
                    name = srcDevice.deviceName,
                    ip = ip,
                    ip_p2p = ip,
                    records = txtRecordMap.toMutableMap(),
                    device = srcDevice
                )
                discoveredServices[srcDevice.deviceName] = serviceInfo
                onDeviceResolved.publish(serviceInfo)
            })

        wifiP2pManager.addServiceRequest(channel,
            WifiP2pDnsSdServiceRequest.newInstance(),
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("WiFiP2P", "Service request added successfully.")
                }

                override fun onFailure(reason: Int) {
                    Log.e("WiFiP2P", "Failed to add service request: ")
                    handleOnActionListenerFailure(reason)
                }
            })

        wifiP2pManager.discoverServices(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WiFiP2P", "Service discovery started.")
            }

            override fun onFailure(reason: Int) {
                Log.e("WiFiP2P", "Failed to start service discovery: ")
                handleOnActionListenerFailure(reason)
            }
        })
    }

    fun stopDiscoveringServices() {
        wifiP2pManager.clearServiceRequests(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WiFiP2P", "Service requests cleared.")
            }

            override fun onFailure(reason: Int) {
                Log.e("WiFiP2P", "Failed to clear service requests: ")
                handleOnActionListenerFailure(reason)
            }
        })
    }

    private val connectionChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            wifiP2pManager.requestConnectionInfo(channel) { info ->
                Log.d(
                    "WiFiDirectConnection",
                    "connectionChangeReceiver. Connected to device. Group owner: $info"
                )
                getConnectedDevices()
                connectionInfo.value = info
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun registerReceivers() {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(peerChangeReceiver, intentFilter)

//        val serviceIntentFilter = IntentFilter().apply {
//            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
//        }
//        context.registerReceiver(serviceChangeReceiver, serviceIntentFilter)

        val connectionIntentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        context.registerReceiver(connectionChangeReceiver, connectionIntentFilter)
    }

    fun unregisterReceivers() {
        context.unregisterReceiver(peerChangeReceiver)
        context.unregisterReceiver(connectionChangeReceiver)
//        context.unregisterReceiver(serviceChangeReceiver)
    }
}

class WifiDirectService : Service() {
    private lateinit var wifiDirectManager: WifiDirectManagerV2
    override fun onCreate() {
        super.onCreate()
        wifiDirectManager = WifiDirectManagerV2(
            applicationContext,
            getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager,
            (getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager).initialize(
                applicationContext, mainLooper, null
            ),
        )
        wifiDirectManager.registerReceivers()
        wifiDirectManager.advertiseService("MyCustomTag")
        wifiDirectManager.discoverServices()
        wifiDirectManager.startDiscoveryLoop()
    }
    override fun onDestroy() {
        super.onDestroy()
        wifiDirectManager.disconnect()
        wifiDirectManager.unregisterReceivers()
        wifiDirectManager.stopDiscoveryLoop()
    }

    private val binder = WifiDirectBinder()

    inner class WifiDirectBinder : Binder() {
        fun getService( ): WifiDirectManagerV2 = wifiDirectManager
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}
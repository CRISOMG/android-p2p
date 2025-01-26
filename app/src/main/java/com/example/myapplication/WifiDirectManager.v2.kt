package com.example.myapplication

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.util.Log
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import java.net.ServerSocket
import java.net.Socket

object  SocketManager {
    val SocketManagerProvider =
        compositionLocalOf<SocketManager> { error("No SocketManager provided") }
    val socketActive = mutableStateOf<Boolean>(value = false)
    val clientSocketActive = mutableStateOf<Boolean>(value = false)
    val serverSocketActive = mutableStateOf<Boolean>(value = false)

    var currSocketClient: Socket? = null
    var currSocketServer: ServerSocket? = null
    fun initServerSocket() {
        Thread {
            try {
                currSocketServer = ServerSocket(8888) // Use any port number
                Log.d("SocketManager", "Server socket initialized. Waiting for clients...")

                val clientSocket = currSocketServer!!.accept() // Wait for client connection
                Log.d("SocketManager", "Client connected: ${clientSocket.inetAddress.hostAddress}")

                // Example: Read and write data
                val inputStream = clientSocket.getInputStream()
                val outputStream = clientSocket.getOutputStream()

                // Handle incoming data (Example: read a message)
                val buffer = ByteArray(1024)
                val bytesRead = inputStream.read(buffer)
                val message = String(buffer, 0, bytesRead)
                Log.d("SocketManager", "Message received: $message")

                // Send a response
                outputStream.write("Hello from server!".toByteArray())

                // Close sockets
                serverSocketActive.value = true
            } catch (e: Exception) {
                Log.e("SocketManager", "Error in server socket: ${e.message}")
            } finally {
            }
        }.start()
    }

    fun initClientSocket(serverAddress: String) {
        Thread {
            try {
                currSocketClient = Socket(serverAddress, 8888) // Use the same port number as the server
                Log.d("SocketManager", "Connected to server: $serverAddress")

                // Example: Send a message to the server
                val outputStream = currSocketClient!!.getOutputStream()
                outputStream.write("Hello from client!".toByteArray())

                // Handle response from server
                val inputStream = currSocketClient!!.getInputStream()
                val buffer = ByteArray(1024)
                val bytesRead = inputStream.read(buffer)
                val response = String(buffer, 0, bytesRead)
                Log.d("SocketManager", "Response from server: $response")
                clientSocketActive.value = true
            } catch (e: Exception) {
                Log.e("SocketManager", "Error in client socket: ${e.message}")
            } finally {
            }
        }.start()
    }

    fun initSockets(info: WifiP2pInfo) {
        if (info.groupFormed) { // Check if a group is formed
            Log.d("SocketInit", "groupFormed.")

            if (info.isGroupOwner) {
                Log.d("SocketInit", "initServerSocket.")

                // Initialize the server socket on the group owner
                initServerSocket()
            } else {
                Log.d("SocketInit", "initClientSocket.")
                // Connect to the server socket on the group owner
                info.groupOwnerAddress?.let { address ->
                    initClientSocket(address.hostAddress)
                }
            }
        } else {
            Log.d("SocketInit", "No group formed yet.")
        }
    }
}
data class ServiceInfo(
    val records: Map<String, Any>,
    val device:  WifiP2pDevice // Use the appropriate type for srcDevice
)
class WifiDirectManagerV2(
    private val context: Context,
    private val wifiP2pManager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val permissionManager: PermissionManager,
) {
    val discoveredDevices = mutableStateListOf<WifiP2pDevice>()

    val discoveredServices = mutableStateMapOf<String, ServiceInfo>()

    fun handleOnActionListenerFailure(reason: Int) {
        when (reason) {
            WifiP2pManager.BUSY -> Log.e("WiFiP2P", "El dispositivo está ocupado.")
            WifiP2pManager.ERROR -> Log.e("WiFiP2P", "Ocurrió un error inesperado.")
            WifiP2pManager.P2P_UNSUPPORTED -> Log.e(
                "WiFiP2P",
                "Wi-Fi Direct no está soportado en este dispositivo."
            )
        }
    }

    fun getDeviceStatusDescription(status: Int): String {
        return when (status) {
            0 -> "available"
            1 -> "connected"
            2 -> "invited but not joined"
            3 -> "invited waiting confirm"
            4 -> "failed"
            5 -> "unavailable"
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
        try {
            val method = WifiP2pManager::class.java.getMethod(
                "deletePersistentGroup",
                WifiP2pManager.Channel::class.java,
                Int::class.javaPrimitiveType,
                WifiP2pManager.ActionListener::class.java
            )

            for (groupId in 0..31) { // IDs usually range from 0 to 31
                method.invoke(
                    wifiP2pManager,
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
        if (true || permissionManager.hasLocationPermissions()) {
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
            permissionManager.requestPermissions()
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
    fun connectToDevice(device: WifiP2pDevice, groupOwnerIntent: Int) {

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC // Use Push Button Configuration (PBC)
            this.groupOwnerIntent = groupOwnerIntent
        }


        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WiFiP2P", "Connection request initiated successfully.")
            }

            override fun onFailure(reason: Int) {
                Log.e("WiFiP2P", "P2P Connection request failed:")
                handleOnActionListenerFailure(reason)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun getConnectedDevices() {
        Log.d(
            "WiFiP2P", "discoveredServices: ${
                discoveredServices.keys.map { key ->
                    val service: ServiceInfo? = discoveredServices[key]
                    mapOf(
                        "r" to service?.records,
                        "d" to service?.device?.deviceName,
                        "status" to service?.device?.status
                    )
                }
            }"
        )
        permissionManager.hasLocationPermissions()
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WiFiP2P", "Discovery started successfully.")
                wifiP2pManager.requestPeers(channel) { peers ->

                    val pt = "10-0050F204-5"
                    val targetDevices = peers.deviceList.filter { d -> d.primaryDeviceType == pt }
                    val devicesString = targetDevices.joinToString {
                        "${it.deviceName} (${
                            getDeviceStatusDescription(it.status)
                        })"
                    }
                    Log.d("WiFiP2P", "[getConnectedDevices]\nDiscovered Devices: $devicesString")

                    discoveredDevices.clear()
                    discoveredDevices.addAll(targetDevices)
                }
            }

            override fun onFailure(reason: Int) {
                handleOnActionListenerFailure(reason)
            }
        })

        wifiP2pManager.requestGroupInfo(channel) { group ->
            if (group != null) {
                val groupOwner = group.owner
                val connectedClients = group.clientList

                Log.d(
                    "WiFiP2P",
                    "Group Owner: ${groupOwner.deviceName} (${groupOwner.deviceAddress})"
                )

                val groupPassword = group.passphrase
                val groupOwnerAddress = group.owner.deviceAddress
                val groupMembers = group.clientList.joinToString { it.deviceName }

                Log.d("WiFiP2P", "Group id: ${group.networkId}")
                Log.d("WiFiP2P", "Group Password: $groupPassword")
                Log.d("WiFiP2P", "Group Owner: $groupOwnerAddress")
                Log.d("WiFiP2P", "Group Members: $groupMembers")
            } else {
                Log.d("WiFiP2P", "No group information available.")
            }
        }
    }

    // BroadcastReceiver for peers
    private val peerChangeReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("WiFiP2P", "[peerChangeReceiver] triggered ${intent?.action}")
        }
    }

    // BroadcastReceiver for services
    private val serviceChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            wifiP2pManager.requestConnectionInfo(channel) { info ->
                // Connection established, use info.groupOwnerAddress
                Log.d("WiFiP2P", "[serviceChangeReceiver]: $info")
//                getConnectedDevices()
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun advertiseService(tag: String) {
        val ip = getLocalIpAddress(context)
        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
            "MyAppService",
            "_myapp._tcp",
            mapOf(
                "tag" to tag,  // Tag for filtering
                "appVersion" to "1.0",
                "ip" to ip
            )
        )

        wifiP2pManager.addLocalService(
            channel,
            serviceInfo,
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

    @SuppressLint("MissingPermission")
    fun discoverServices() {
        Log.d("WiFiP2P", "[discoverServices]")
        wifiP2pManager.setDnsSdResponseListeners(
            channel,
            { instanceName, registrationType, device ->
                Log.d("WiFiP2P", "[discoverServices] setDnsSdResponseListeners response")
                if (instanceName == "MyAppService") {
                    Log.d(
                        "WiFiP2P",
                        "Service discovered: $instanceName on device: ${device.deviceName}"
                    )
                    discoveredDevices.clear() // Add device to your list
                    discoveredDevices.addAll(listOf(device)) // Add device to your list
                }
            },
            { fullDomainName, txtRecordMap, srcDevice ->
                Log.d("WiFiP2P", "Service TXT records: $txtRecordMap")

                val serviceInfo = ServiceInfo(
                    records = txtRecordMap,
                    device = srcDevice
                )
                discoveredServices[srcDevice.deviceName] = serviceInfo
            }
        )

        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
        wifiP2pManager.addServiceRequest(
            channel,
            serviceRequest,
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
                // Connection established, use info.groupOwnerAddress
                Log.d(
                    "WiFiDirectConnection",
                    "connectionChangeReceiver. Connected to device. Group owner: $info"
                )
                getConnectedDevices()
//                socketManager.initSockets(info)
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

        val serviceIntentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        }
        context.registerReceiver(serviceChangeReceiver, serviceIntentFilter)

        val connectionIntentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        context.registerReceiver(connectionChangeReceiver, connectionIntentFilter)
    }

    fun unregisterReceivers() {
        context.unregisterReceiver(peerChangeReceiver)
        context.unregisterReceiver(serviceChangeReceiver)
    }
}

package com.devcrisomg.wifip2p_custom_app


import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket


@Composable
fun ProvideSocketManager(
    context: Context, wifiP2PManager: WifiDirectManagerV2, content: @Composable () -> Unit
) {
    SocketManager.init(context, wifiP2PManager)
    CompositionLocalProvider(SocketManagerProvider provides SocketManager) {
        content()
    }
}

val SocketManagerProvider = compositionLocalOf<SocketManager> { error("No SocketManager provided") }

data class ClientInfo(val socket: Socket, val macAddress: String?)
object SocketManager {
    @SuppressLint("StaticFieldLeak")
    private lateinit var wifiDirectManager: WifiDirectManagerV2


    val socketActive = mutableStateOf<Boolean>(value = false)
    val clientSocketActive = mutableStateOf<Boolean>(value = false)
    val serverSocketActive = mutableStateOf<Boolean>(value = false)
    val socketIp = mutableStateOf<String?>(value = null)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    var currSocketClient: Socket? = null
    var currSocketServer: ServerSocket? = null

    val connectedClients = mutableStateListOf<ClientInfo>()
    val connectedSockets = mutableStateMapOf<String, Boolean>()

    val receivedMessages = mutableStateListOf<String>() // Lista observable para mensajes
    val sentMessages = mutableStateListOf<String>() // Mensajes enviados


    private var appContext: Context? = null

    fun init(context: Context, wifiDirectManager: WifiDirectManagerV2) {
        appContext = context.applicationContext
        this.wifiDirectManager = wifiDirectManager
        initServerSocket()
    }

    @SuppressLint("SuspiciousIndentation")
    fun initServerSocket() {
        Log.d("SocketManager", "Initializing Server Socket.")
        if (serverSocketActive.value) {
            Log.d("SocketManager", "Server socket is already initialized.")
            return
        }
        Thread {
            try {
                val s = ServerSocket(8888)
                Log.d("SocketManager", "Server socket initialized.")
                currSocketServer = s
                serverSocketActive.value = true
                while (true) {
                    val client = currSocketServer!!.accept()
                    val clientIp = client.inetAddress.hostAddress
//                    if (connectedClients.none { it.socket.inetAddress.hostAddress == clientIp }) {
                    connectedClients.add(ClientInfo(client, macAddress = ""))
                    Log.d("SocketManager", "New client connected: $clientIp")
//                    } else {
//                        Log.d("SocketManager", "Client $clientIp already connected, ignoring.")
//                    }
                    listenForMessages(client)
                }
            } catch (e: Exception) {
                Log.d("SocketManager", "ServerSocket Error: ${e.message}")
                serverSocketActive.value = false
            } finally {
                Log.d("SocketManager", "ServerSocket Thread Finished")
            }
        }.start()
    }

    fun initClientSocket(serverAddress: String, macAddress: String) {
        Thread {
            try {
                Log.d("SocketManager", "Try to Connect to $serverAddress.")

                val s = Socket(serverAddress, 8888)
                currSocketClient = s

//                val output = s.getOutputStream().bufferedWriter()
//                output.write("$macAddress\n") // Enviar el payload seguido de un salto de línea
//                output.flush()

                Log.d("SocketManager", "SocketClient connected to SocketServer.")

                socketIp.value = s.localAddress.hostAddress
//                Log.d("SocketManager", "IIIIII ${socketIp.value} ${macAddress}.")

                clientSocketActive.value = true
                connectedSockets[serverAddress] = true
                listenForMessages(currSocketClient!!)
            } catch (e: Exception) {
                Log.d("SocketManager", "Error iniciando cliente: ${e.message}")
                clientSocketActive.value = false
            } finally {
                Log.d("SocketManager", "ClientSocket Thread Finished")
            }
        }.start()
    }

    fun auth(authPayload: String): Boolean {
        val wasDiscovered = wifiDirectManager.discoveredServices.values.find {
            Log.d(
                "SocketManager",
                "deviceAddress: ${it.device.deviceAddress.toString()} PAYLOAD $authPayload"
            )
            it.device.deviceAddress.equals(authPayload)
        }
        Log.d("SocketManager", "AUTH: ${wasDiscovered} PAYLOAD $authPayload")
        return wasDiscovered != null
    }

    private fun listenForMessages(socket: Socket) {
        Thread {
            try {
                val input = socket.getInputStream().bufferedReader()
//                if (!auth(input.readLine())) {
//                    socket.close()
//                } else {
//
//                }
                while (true) {
                    val message = input.readLine()
                    if (message != null) {
                        Log.d("SocketManager", "Mensaje recibido: $message")

                        appContext?.let { context ->
                            mainScope.launch {
                                receivedMessages.add(message)
                                Toast.makeText(context, "msj received", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } else {
                        break // Salir del bucle si se recibe null
                    }
                }
            } catch (e: Exception) {
                Log.d("SocketManager", "Error escuchando mensajes: ${e.message}")

            }
        }.start()
    }

    fun sendMessage(message: String, socket: Socket?) {
        Thread {
            try {
                socket?.getOutputStream()?.write("$message\n".toByteArray())

                mainScope.launch {
                    sentMessages.add(message)
                }
            } catch (e: Exception) {
                Log.d("SocketManager", "Error sendMessage: ${e.message}")
                closeSockets()
                appContext?.let { context ->
                    mainScope.launch {
                        Toast.makeText(context, "Availability Error", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }.start()
    }

    fun closeSockets() {
        Log.d("SocketManager", "Closing sockets...")
        try {
            currSocketClient?.close()
//            currSocketServer?.close()
            sentMessages.clear()
            receivedMessages.clear()
            connectedClients.clear()
            connectedSockets.clear()
            clientSocketActive.value = false
//            serverSocketActive.value = false
            Log.d("SocketManager", "Sockets closed")
        } catch (e: Exception) {
            Log.e("SocketManager", "Error closing sockets: ${e.message}")
        }
    }

    fun getMacAddressFromSocket(socket: Socket): String? {
        // Get the InetAddress from the Socket
        val inetAddress: InetAddress = socket.inetAddress

        // Get the Network Interface associated with the IP address
        val networkInterface: NetworkInterface? = NetworkInterface.getByInetAddress(inetAddress)

        return if (networkInterface != null) {
            // Retrieve the MAC address as a byte array
            val macBytes: ByteArray? = networkInterface.hardwareAddress
            // Format the MAC address to a human-readable string
            macBytes?.joinToString(":") { String.format("%02X", it) }
        } else {
            null
        }
    }

    fun LoopNetI() {
        val networkInterfaces = NetworkInterface.getNetworkInterfaces()

        while (networkInterfaces.hasMoreElements()) {
            val networkInterface = networkInterfaces.nextElement()
            val inetAddresses = networkInterface.inetAddresses

            while (inetAddresses.hasMoreElements()) {
                val address = inetAddresses.nextElement()
                Log.d("IPLOOP", "${address}")

            }
        }
    }

    fun getMacAddressByIp(ip: String): String? {
        return try {
            val inetAddress = InetAddress.getByName(ip)
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()

            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val inetAddresses = networkInterface.inetAddresses

                while (inetAddresses.hasMoreElements()) {
                    val address = inetAddresses.nextElement()
                    if (address.equals(inetAddress)) {
                        val macBytes = networkInterface.hardwareAddress
                        return macBytes?.joinToString(":") { String.format("%02X", it) }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("MAC Address", "Error retrieving MAC address: ${e.message}")
            null
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocketMessagesContainer(modifier: Modifier = Modifier) {
    var messageToSend by remember { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<Socket?>(null) }
    val socketManager = SocketManagerProvider.current

    LaunchedEffect(Unit) {
        if (socketManager.connectedClients.isNotEmpty()) {
            selectedClient = socketManager.connectedClients.first().socket
        }
    }

    val listState = rememberLazyListState()
    var isUserScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(socketManager.receivedMessages.size) {
        if (socketManager.receivedMessages.isNotEmpty() && !isUserScrolling) {
            listState.animateScrollToItem(socketManager.receivedMessages.size - 1)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect {
                val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                isUserScrolling = lastVisibleIndex < socketManager.receivedMessages.size - 1
            }
    }

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp), state = listState
        ) {
            items(socketManager.receivedMessages) { message ->
                Text(text = message)
            }
        }

        LazyColumn(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            items(socketManager.connectedClients) { client ->
                Text(text = if (client.socket.inetAddress.hostAddress == selectedClient?.inetAddress?.hostAddress) "${client.socket.inetAddress.hostAddress ?: "Cliente desconocido"} <<<"
                else client.socket.inetAddress.hostAddress,
                    modifier = Modifier.clickable { selectedClient = client.socket })
            }
        }
        fun handleSendToClient() {
            if (messageToSend.isNotBlank()) {
                socketManager.sendMessage(messageToSend, selectedClient)
            }
        }

        fun handleSendToServer() {
            if (messageToSend.isNotBlank()) {
                socketManager.sendMessage(messageToSend, socketManager.currSocketClient)
            }
        }
        FlowRow(

            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            CustomButton(
                onClick = {
                    handleSendToServer()
                }, text = "Server", modifier = Modifier
            )
            Spacer(modifier = Modifier.width(4.dp))
            CustomButton(
                onClick = {
                    handleSendToClient()
                }, text = "Client", modifier = Modifier
            )
            Spacer(modifier = Modifier.width(4.dp))
            CustomButton(
                onClick = {
                    socketManager.receivedMessages.clear()
                }, text = "Clear Messages", modifier = Modifier
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                label = { Text("Enviar Mensaje") },
                value = messageToSend,
                onValueChange = { messageToSend = it },
                placeholder = { Text("Escribe un mensaje...") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = {
                    if (selectedClient?.isConnected == true) {
                        handleSendToClient()
                    } else {
                        handleSendToServer()
                    }
                    messageToSend = ""
                }),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}
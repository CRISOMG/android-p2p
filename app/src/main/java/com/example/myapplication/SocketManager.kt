package com.example.myapplication


import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.net.Socket


@Composable
fun ProvideSocketManager(content: @Composable () -> Unit) {
    CompositionLocalProvider(SocketManagerProvider provides SocketManager) {
        content()
    }
}

val SocketManagerProvider =
    compositionLocalOf<SocketManager> { error("No SocketManager provided") }

object SocketManager {
    val socketActive = mutableStateOf<Boolean>(value = false)
    val clientSocketActive = mutableStateOf<Boolean>(value = false)
    val serverSocketActive = mutableStateOf<Boolean>(value = false)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    var currSocketClient: Socket? = null
    var currSocketServer: ServerSocket? = null

    val connectedClients = mutableStateListOf<Socket>()
    val connectedSockets = mutableStateMapOf<String, Boolean>()

    val receivedMessages = mutableStateListOf<String>() // Lista observable para mensajes
    val sentMessages = mutableStateListOf<String>() // Mensajes enviados

    fun initServerSocket() {
        Log.d("SocketManager", "Initializing Server Socket.")
        if (serverSocketActive.value) {
            Log.d("SocketManager", "Server socket is already initialized.")
            return
        }
        Thread {
            try {
                currSocketServer = ServerSocket(8888)
                Log.d("SocketManager", "Server socket initialized.")
                serverSocketActive.value = true
                while (true) {
                    val client = currSocketServer!!.accept()
                    val clientIp = client.inetAddress.hostAddress
                    if (connectedClients.none { it.inetAddress.hostAddress == clientIp }) {
                        connectedClients.add(client)
                        Log.d("SocketManager", "New client connected: $clientIp")
                    } else {
                        Log.d("SocketManager", "Client $clientIp already connected, ignoring.")
                    }
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

    fun initClientSocket(serverAddress: String) {
        Thread {
            try {
                currSocketClient = Socket(serverAddress, 8888)
                Log.d("SocketManager", "SocketClient connected to SocketServer.")
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

    private fun listenForMessages(socket: Socket) {
        Thread {
            try {
                val input = socket.getInputStream().bufferedReader()
                while (true) {
                    val message = input.readLine()
                    if (message != null) {
                        mainScope.launch {
                            receivedMessages.add(message)
                        }
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
            }
        }.start()
    }

    fun closeSockets() {
        Log.d("SocketManager", "Closing sockets...")
        try {
            currSocketClient?.close()
            currSocketServer?.close()
            sentMessages.clear()
            receivedMessages.clear()
            connectedClients.clear()
            connectedSockets.clear()
            clientSocketActive.value = false
            serverSocketActive.value = false
            Log.d("SocketManager", "Sockets closed")
        } catch (e: Exception) {
            Log.e("SocketManager", "Error closing sockets: ${e.message}")
        }
    }
}

@Composable
fun ClientUI(socketManager: SocketManager, modifier: Modifier = Modifier) {
    var messageToSend by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 200.dp)
                .fillMaxWidth()
        ) {
            items(socketManager.receivedMessages) { message ->
                Text(text = message)
            }
        }

        fun HandleSendMessage() {
            if (messageToSend.isNotBlank()) {
                socketManager.sendMessage(messageToSend, socketManager.currSocketClient)
                //messageToSend = ""
            }
        }
        Row {
            OutlinedTextField(
                label = { Text("Enviar al servidor") },
                value = messageToSend,
                onValueChange = { messageToSend = it },
                placeholder = { Text("Escribe un mensaje...") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        HandleSendMessage()
                        messageToSend = ""
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    CustomButton(
                        onClick = {
                            HandleSendMessage()
                        },
                        text = "Enviar",
                        modifier = Modifier.padding(PaddingValues(end = 8.dp))

                    )
                }
            )
        }
    }
}

@Composable
fun ServerUI(socketManager: SocketManager, modifier: Modifier = Modifier) {
    var messageToSend by remember { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<Socket?>(null) }

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 200.dp)
                .fillMaxWidth(),
        ) {
            items(socketManager.connectedClients) { client ->
                Text(
                    text =
                    if (client.inetAddress.hostAddress == selectedClient?.inetAddress?.hostAddress)
                        "${client.inetAddress.hostAddress ?: "Cliente desconocido"} <<<"
                    else client.inetAddress.hostAddress,
                    modifier = Modifier.clickable { selectedClient = client }
                )
            }
        }
        fun handleSendMessage() {
            socketManager.sendMessage(messageToSend, selectedClient)
        }
        OutlinedTextField(
            label = { Text("Enviar al cliente") },
            value = messageToSend,
            onValueChange = { messageToSend = it },
            placeholder = { Text("Escribe un mensaje...") },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    handleSendMessage()
                    messageToSend = ""
                }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                CustomButton(
                    onClick = {
                        handleSendMessage()
                    },
                    text = "Enviar",
                    modifier = Modifier.padding(PaddingValues(end = 8.dp))
                )
            }
        )
    }
}
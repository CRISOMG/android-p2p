package com.example.myapplication

//import com.google.accompanist.flowlayout.FlowRow
//import androidx.compose.foundation.layout.FlowRow
//import com.google.accompanist.flowlayout.FlowRow
import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

//import com.google.accompanist.flowlayout.FlowRow

@Composable
fun CustomButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String = "Placeholder Text",
    containerColor: Color = Color(0xffff7f50),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentPadding: PaddingValues = PaddingValues(4.dp),
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(), // Permite personalizar la elevación
    border: BorderStroke? = null, // Permite agregar un borde opcional
    enabled: Boolean = true, // Controla si el botón está habilitado
) {
    Button(
        onClick = onClick,
        modifier = modifier.wrapContentSize(),
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor, contentColor = contentColor
        ),
        shape = MaterialTheme.shapes.extraSmall,
        elevation = elevation,
        border = border, // Usa el borde personalizado
        enabled = enabled, // Controla si el botón está habilitado
        // Add rounded corners (shape defined in your theme)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.wrapContentWidth(),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            color = Color.Black
        )
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var wifiDirectManager: WifiDirectManagerV2
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)
        wifiDirectManager = WifiDirectManagerV2(
            this,
            getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager,
            (getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager).initialize(
                this, mainLooper, null
            ),
            permissionManager = permissionManager
        )

        permissionManager.requestPermissions()
        Log.d("MainActivity", "fsadfasdfasdfsadfsaf")
        wifiDirectManager.registerReceivers()
        wifiDirectManager.advertiseService("MyCustomTag")
        wifiDirectManager.discoverServices()
//        wifiDirectManager.startDiscoveryLoop()

        setContent {
            MyApplicationTheme(
                darkTheme = true, dynamicColor = false
            ) {
                MainScreen(devices = wifiDirectManager.discoveredServices,
                    wifiP2PManager = wifiDirectManager,
                    onDiscoverDevices = {
                        Log.d("MainActivity", "Discover Devices button clicked")
                        wifiDirectManager.discoveredServices.clear()
                        wifiDirectManager.getConnectedDevices()
                    },
                    onDeviceClick = { device, priori ->
                        Log.d("MainActivity", "Clicked on device: ${device.deviceName}")
                        wifiDirectManager.connectToDevice(device, priori)
                    })
            }
        }
    }

    override fun onResume() {
        super.onResume()
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

    override fun onPause() {
        super.onPause()
//        wifiDirectManager.unregisterReceivers()
        wifiDirectManager.stopDiscoveryLoop()
        wifiDirectManager.stopAdvertisingService()
        wifiDirectManager.stopDiscoveringServices()
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    devices: MutableMap<String, ServiceInfo>,
    wifiP2PManager: WifiDirectManagerV2,
    onDiscoverDevices: () -> Unit,
    onDeviceClick: (WifiP2pDevice, Int) -> Unit
) {
    val output = remember { mutableStateOf("") }
    ProvideSocketManager {
        CompositionLocalProvider(
            WifiP2PManagerProvider provides wifiP2PManager
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()) // Make the entire content scrollable
                ) {
                    // FlowRow for buttons
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.Top
                    ) {
                        CustomButton(
                            onClick = {
                                wifiP2PManager.stopAdvertisingService()
                                wifiP2PManager.stopDiscoveringServices()
                                wifiP2PManager.registerReceivers()
                                wifiP2PManager.advertiseService("MyCustomTag")
                                wifiP2PManager.discoverServices()
                            }, text = "Discovery", modifier = Modifier.padding(2.dp)
                        )

                        CustomButton(
                            onClick = onDiscoverDevices,
                            text = "Refresh",
                            modifier = Modifier.padding(2.dp)
                        )
                        CustomButton(
                            onClick = { wifiP2PManager.cancelConnectionToDevice() },
                            text = "Cancel Connection",
                            modifier = Modifier.padding(2.dp)
                        )
                        CustomButton(
                            onClick = { wifiP2PManager.removePersistentGroups() },
                            text = "Force remove groups",
                            modifier = Modifier.padding(2.dp)
                        )

                        if (wifiP2PManager.connectionInfo.value?.groupFormed == true) {
                            CustomButton(
                                onClick = { wifiP2PManager.disconnect() },
                                text = "Remove group",
                                modifier = Modifier.padding(2.dp)
                            )
                        } else {
                            CustomButton(
                                onClick = { wifiP2PManager.createGroup() },
                                text = "Create group",
                                modifier = Modifier.padding(2.dp)
                            )
                        }

                        val socketManager = SocketManagerProvider.current
                        CustomButton(
                            onClick = {
                                if (!socketManager.serverSocketActive.value) {
                                    socketManager.initServerSocket()
                                } else {
                                    try {
                                        socketManager.closeSockets()
                                        Log.d("SocketManager", "Server sockets closed.")
                                    } catch (e: Exception) {
                                        Log.e(
                                            "SocketManager",
                                            "Error closing server socket: ${e.message}"
                                        )
                                    }
                                }
                            },
                            text = if (socketManager.serverSocketActive.value) "Close Server Socket" else "Create Server Socket",
                            modifier = Modifier.padding(2.dp)
                        )
                    }

                    // Logcat and DeviceList
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("LOGCAT:")

                        val scrollState = rememberScrollState()
                        val coroutineScope = rememberCoroutineScope()

                        LaunchedEffect(output.value) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(scrollState)
                                .padding(8.dp)
                        ) {
                            Text(output.value, style = MaterialTheme.typography.bodySmall)
                        }

                        Row {
                            CustomButton(
                                onClick = { output.value = ""; handleSetLogcat(output) },
                                text = "Capture Logcat"
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            CustomButton(onClick = { output.value = "" }, text = "Clear")
                            Spacer(modifier = Modifier.width(16.dp))
                            CustomButton(onClick = {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }, text = "Down")
                        }
                    }

                    DeviceList(
                        devices = devices,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .padding(16.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val socketManager = SocketManagerProvider.current
                        Row {
                            ServerUI(socketManager, modifier = Modifier.heightIn(max = 400.dp))
                        }
                        Row {
                            ClientUI(socketManager, modifier = Modifier.heightIn(max = 400.dp))
                        }
                    }
                }
            }
        }
    }


}




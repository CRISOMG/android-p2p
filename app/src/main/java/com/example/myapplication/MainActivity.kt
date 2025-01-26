package com.example.myapplication

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.SocketManager.SocketManagerProvider

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
            (getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager)
                .initialize(this, mainLooper, null),
            permissionManager = permissionManager
        )

        permissionManager.requestPermissions()
        Log.d("MainActivity", "fsadfasdfasdfsadfsaf")
        wifiDirectManager.registerReceivers()
        wifiDirectManager.advertiseService("MyCustomTag")
        wifiDirectManager.discoverServices()

        setContent {
            MyApplicationTheme {
                MainScreen(
                    devices = wifiDirectManager.discoveredServices,
                    wifiP2PManager = wifiDirectManager,
                    onDiscoverDevices = {
                        Log.d("MainActivity", "Discover Devices button clicked")
//                        wifiDirectManager.advertiseService("MyCustomTag")
//                        wifiDirectManager.discoverServices()
                        wifiDirectManager.getConnectedDevices()
                    },
                    onDeviceClick = { device, priori ->
                        // Handle device click, e.g., connect to the device
                        Log.d("MainActivity", "Clicked on device: ${device.deviceName}")
                        wifiDirectManager.connectToDevice(device, priori)
                    })
            }
        }
    }

    override fun onResume() {
        super.onResume()
//        wifiDirectManager.discoverDevices()
        wifiDirectManager.registerReceivers()
        wifiDirectManager.advertiseService("MyCustomTag")
        wifiDirectManager.discoverServices()
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiDirectManager.disconnect()
        wifiDirectManager.unregisterReceivers()
    }

    override fun onPause() {
        super.onPause()
//        wifiDirectManager.unregisterReceivers()
        wifiDirectManager.stopAdvertisingService()
        wifiDirectManager.stopDiscoveringServices()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    devices: MutableMap<String, ServiceInfo>,
    wifiP2PManager: WifiDirectManagerV2,
    onDiscoverDevices: () -> Unit,
    onDeviceClick: (WifiP2pDevice, Int) -> Unit
) {
    val socketManager = remember { SocketManager }
    CompositionLocalProvider(
        SocketManagerProvider provides socketManager
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Button(
                    onClick = {
//                    wifiP2PManager.unregisterReceivers()
                        wifiP2PManager.stopAdvertisingService()
                        wifiP2PManager.stopDiscoveringServices()

                        wifiP2PManager.registerReceivers()
                        wifiP2PManager.advertiseService("MyCustomTag")
                        wifiP2PManager.discoverServices()
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xffff7f50),
                        contentColor = Color.Black,
                    )
                ) {
                    Text(
                        text = "Discovery",
                        color = Color.Black,
                        style = TextStyle(
                            color = Color.Black
                        )
                    )

                }
                Button(
                    onClick = onDiscoverDevices,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xffff7f50),
                        contentColor = Color.Black,
                    )
                ) {
                    Text(
                        text = "Refresh",
                        color = Color.Black,
                        style = TextStyle(
                            color = Color.Black
                        )
                    )

                }
                Button(
                    onClick = { wifiP2PManager.cancelConnectionToDevice() },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xffff7f50),
                        contentColor = Color.Black,
                    )
                ) {
                    Text(
                        text = "Cancel Connection",
                        color = Color.Black,
                        style = TextStyle(color = Color.Black)
                    )
                }
                Button(
                    onClick = { wifiP2PManager.removePersistentGroups() },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xffff7f50),
                        contentColor = Color.Black,
                    )
                ) {
                    Text(
                        text = "Force remove groups",
                        color = Color.Black,
                        style = TextStyle(color = Color.Black)
                    )
                }
                Button(
                    onClick = { wifiP2PManager.disconnect() },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xffff7f50),
                        contentColor = Color.Black,
                    )
                ) {
                    Text(
                        text = "Remove group",
                        color = Color.Black,
                        style = TextStyle(color = Color.Black)
                    )
                }
                Button(
                    onClick = { wifiP2PManager.createGroup() },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xffff7f50),
                        contentColor = Color.Black,
                    )
                ) {
                    Text(
                        text = "Create group",
                        color = Color.Black,
                        style = TextStyle(color = Color.Black)
                    )
                }

                Button(
                    onClick = {
                        if (!socketManager.serverSocketActive.value) {
                            socketManager.initServerSocket()
                        } else {
                            socketManager.currSocketServer?.close()
                            socketManager.currSocketServer?.accept()?.close()
                        }
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xffff7f50),
                        contentColor = Color.Black,
                    )
                ) {
                    Text(
                        text = socketManager.socketActive.value.let {
                            if (it) "Close Server Socket" else "Create Server Socket"
                        } ,
                        color = Color.Black,
                        style = TextStyle(color = Color.Black)
                    )
                }

                // Device list below the button
                DeviceList(
                    devices = devices,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

}




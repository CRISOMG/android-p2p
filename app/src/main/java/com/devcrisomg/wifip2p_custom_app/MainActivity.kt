package com.devcrisomg.wifip2p_custom_app

//import com.google.accompanist.flowlayout.FlowRow
//import androidx.compose.foundation.layout.FlowRow
//import com.google.accompanist.flowlayout.FlowRow
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devcrisomg.wifip2p_custom_app.ui.theme.MyApplicationTheme

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
    private var isServiceConnected = mutableStateOf(false)

    private val wifiDirectServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WifiDirectService.WifiDirectBinder
            wifiDirectManager = binder.getService()
            isServiceConnected.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceConnected.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)
        permissionManager.requestPermissions()
//        wifiDirectManager = WifiDirectManagerV2(
//            this,
//            getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager,
//            (getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager).initialize(
//                this, mainLooper, null
//            ),
//            permissionManager = permissionManager
//        )

        val serviceIntent = Intent(this, WifiDirectService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, wifiDirectServiceConnection, Context.BIND_AUTO_CREATE)



        setContent {
            MyApplicationTheme(
                darkTheme = true, dynamicColor = false
            ) {
                if (isServiceConnected.value) {
                    MainScreen(wifiP2PManager = wifiDirectManager)
                } else {
                    // Show a loading indicator or placeholder until the service is connected
                    Text("Initializing...")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isServiceConnected.value) {
            wifiDirectManager.registerReceivers()
            wifiDirectManager.advertiseService("MyCustomTag")
            wifiDirectManager.discoverServices()
            wifiDirectManager.startDiscoveryLoop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(wifiDirectServiceConnection)
        if (isServiceConnected.value) {
            wifiDirectManager.disconnect()
            wifiDirectManager.unregisterReceivers()
            wifiDirectManager.stopDiscoveryLoop()
        }
    }

    override fun onPause() {
        super.onPause()
//        wifiDirectManager.unregisterReceivers()
        if (isServiceConnected.value) {
            wifiDirectManager.stopDiscoveryLoop()
            wifiDirectManager.stopAdvertisingService()
            wifiDirectManager.stopDiscoveringServices()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenA(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->

        val wifiP2PManager = WifiP2PManagerProvider.current

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
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),

                ) {


                CustomButton(
                    onClick = {
                        Log.d("MainActivity", "Discover Devices button clicked")
                        wifiP2PManager.cancelConnectionToDevice()
                        wifiP2PManager.removePersistentGroups()

                        wifiP2PManager.stopAdvertisingService()
                        wifiP2PManager.stopDiscoveringServices()
                        wifiP2PManager.unregisterReceivers()

                        wifiP2PManager.registerReceivers()
                        wifiP2PManager.advertiseService("MyCustomTag")
                        wifiP2PManager.discoverServices()


                        wifiP2PManager.getConnectedDevices()
                    },
                    text = "Refresh",
                    modifier = Modifier
                )
                CustomButton(
                    onClick = {
                        wifiP2PManager.discoveredServices.clear()
                    }, text = "Clear Discovery", modifier = Modifier
                )

                if (wifiP2PManager.connectionInfo.value?.groupFormed == true) {
                    CustomButton(
                        onClick = { wifiP2PManager.disconnect() },
                        text = "Remove group",
                        modifier = Modifier
                    )
                } else {
                    CustomButton(
                        onClick = { wifiP2PManager.createGroup() },
                        text = "Create group",
                        modifier = Modifier
                    )
                }
            }
            CustomButton(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { navController.navigate("screenB") },
                text = "LOGCAT"
            )
            val socketManager = SocketManagerProvider.current
            FlowRow(
                modifier = Modifier.padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
//                LaunchedEffect(Unit) {
//                    Log.d("SocketManager", "OE!")
//                    if (!socketManager.serverSocketActive.value) {
//                    Log.d("SocketManager", "OE!")

//                        socketManager.initServerSocket()
//                    }
//                }
                CustomButton(
                    modifier = Modifier,
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
                )
                CustomButton(
                    modifier = Modifier,
                    onClick = { navController.navigate("screenC") },
                    text = "Socket Messages"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            ) {
                if (wifiP2PManager.connectionInfo.value?.groupFormed == true) {
                    if (wifiP2PManager.connectionInfo.value?.isGroupOwner == true) {
                        Text("You are Group Owner of the WIFI P2P Group")
                    } else {
                        Text("You are in a WIFI P2P Group")
                    }
                }
            }
            DeviceList(
                devices = wifiP2PManager.discoveredServices,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            )

        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenB(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Make the entire content scrollable
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CustomButton(
                    onClick = { navController.navigate("screenA") },
                    text = "<--"
                )
            }
            LogCatList(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenC(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Make the entire content scrollable
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CustomButton(
                    onClick = { navController.navigate("screenA") },
                    text = "<--"
                )
            }
            SocketMessagesContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    wifiP2PManager: WifiDirectManagerV2,
) {
    val context = LocalContext.current
    ProvideSocketManager(context = context, wifiP2PManager) {
        ProvideWifiDirectManager(
            wifiP2PManager
        ) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "screenA") {
                composable("screenA") { ScreenA(navController) }
                composable("screenB") { ScreenB(navController) }
                composable("screenC") { ScreenC(navController) }
//                composable(
//                    "screenB/{key}",
//                    arguments = listOf(navArgument("key") { type = NavType.StringType })
//                ) { backStackEntry ->
//                    val key = backStackEntry.arguments?.getString("key")
//                    ScreenB(key)
//                }
            }

        }
    }
}




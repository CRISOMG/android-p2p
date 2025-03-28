package com.devcrisomg.wifip2p_custom_app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devcrisomg.wifip2p_custom_app.components.CustomButton
import com.devcrisomg.wifip2p_custom_app.components.DeviceEventBus
import com.devcrisomg.wifip2p_custom_app.components.DeviceInfoModel
import com.devcrisomg.wifip2p_custom_app.components.DeviceList
import com.devcrisomg.wifip2p_custom_app.components.DeviceViewModel
import com.devcrisomg.wifip2p_custom_app.components.LogCatList
import com.devcrisomg.wifip2p_custom_app.controllers.NsdController
import com.devcrisomg.wifip2p_custom_app.controllers.PermissionManager
import com.devcrisomg.wifip2p_custom_app.controllers.ProvideSocketManager
import com.devcrisomg.wifip2p_custom_app.controllers.ProvideWifiDirectManager
import com.devcrisomg.wifip2p_custom_app.controllers.SocketManagerProvider
import com.devcrisomg.wifip2p_custom_app.controllers.SocketMessagesContainer
import com.devcrisomg.wifip2p_custom_app.controllers.WifiDirectManagerV2
import com.devcrisomg.wifip2p_custom_app.controllers.WifiDirectService
import com.devcrisomg.wifip2p_custom_app.controllers.WifiP2PManagerProvider
import com.devcrisomg.wifip2p_custom_app.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var wifiDirectManager: WifiDirectManagerV2
    private lateinit var permissionManager: PermissionManager
    private lateinit var nsdController: NsdController
//    private lateinit var customUpdateManager: CustomUpdateManager
    private var isServiceConnected = mutableStateOf(false)


    @Inject
    lateinit var deviceEventBus: DeviceEventBus;

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

        val pm = packageManager
        val info = pm.getPackageInfo(packageName, 0)
        val currentVersion = info.versionName
        Log.d("GeneralLog", "$currentVersion")

//        val privateDir: File? = getExternalFilesDir(null)
//        if (privateDir != null) {
//            Log.d("GeneralLog", "Ruta del directorio privado: ${privateDir.absolutePath}")
//        }

//        customUpdateManager = CustomUpdateManager(this)
        nsdController = NsdController(this,)
        permissionManager = PermissionManager(this)
        permissionManager.requestPermissions()


        val serviceIntent = Intent(this, WifiDirectService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, wifiDirectServiceConnection, Context.BIND_AUTO_CREATE)


       try {
           val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
           wifiManager.createMulticastLock("mDNSLock").acquire()
       } catch (e: Exception) {

           e.message?.let { Log.e("NsdManager", it) }
           Toast.makeText(this, "wifiManager.createMulticastLock error", Toast.LENGTH_SHORT).show()
       }
//        nsdController.startPeriodicAdvertising()


        setContent {
            MyApplicationTheme(
                darkTheme = true, dynamicColor = false
            ) {
                if (isServiceConnected.value) {

                    LaunchedEffect(Unit) {
                    nsdController.startDiscovery()
                    nsdController.advertiseService()
                    launch {
                            nsdController.onDeviceResolved.events.collect { event ->
                                event.let {
                                    Log.d("NsdManager", "nsdController.onDeviceResolved.subscribe ${event.name}  ${event.ip}")
                                    deviceEventBus.publish(event)
                                }
                            }
                        }
                        launch {
                            wifiDirectManager.onDeviceResolved.events.collect { event ->
                                event.let {
                                    Log.d("WiFiP2P", "wifiDirectManager.onDeviceResolved.subscribe ${event.name} ${event.ip}")
                                    deviceEventBus.publish(DeviceInfoModel(
                                        name = event.name,
                                        ip = event.ip,
                                        ip_p2p = event.ip,
                                        device = event.device
                                    ))
                                }
                            }
                        }
                    }



                    MainScreen(
                        wifiP2PManager = wifiDirectManager,
//                        customUpdateManager,
                        mdnsAdvertise = { nsdController.advertiseService() }
                        )
                } else {
                    Text("Initializing...")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
//        nsdController.advertiseService()
//        nsdController.startDiscovery()
        if (isServiceConnected.value) {
//            wifiDirectManager.registerReceivers()
//            wifiDirectManager.advertiseService("MyCustomTag")
//            wifiDirectManager.discoverServices()
//            wifiDirectManager.startDiscoveryLoop()
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
fun ScreenA(navController: NavController,
//            customUpdateManager: CustomUpdateManager,
            mdnsAdvertise: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->

        val mainActivity = (LocalContext.current) as MainActivity
        val viewModel: DeviceViewModel = hiltViewModel(mainActivity)
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
                    containerColor = Color(0xFF2196F3),
                    onClick = {
                        mdnsAdvertise()
                    }, text = "Advertise", modifier = Modifier
                )


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
                        viewModel.clearDevices()
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

            CustomButton(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
//                    customUpdateManager.showApiUrlDialog()
                },
                text = "Check Updates"
            )

            CustomButton(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
//                    Log.d("GeneralLog", customUpdateManager.getCurrDownloadManaged().toString())
                },
                text = "DEBUG"
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
//    customUpdateManager: CustomUpdateManager,
    mdnsAdvertise: () -> Unit
) {
    val context = LocalContext.current
    ProvideSocketManager(context = context, wifiP2PManager) {
        ProvideWifiDirectManager(
            wifiP2PManager
        ) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "screenA") {
                composable("screenA") { ScreenA(navController, mdnsAdvertise) }
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




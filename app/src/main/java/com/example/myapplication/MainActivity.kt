package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme


import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
//import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
//import android.os.Build
import android.util.Log
import android.widget.Toast
// import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateListOf
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.FlowColumnScopeInstance.align
//import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
//import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
//import android.provider.Settings


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun DeviceList(devices: List<WifiP2pDevice>, modifier: Modifier = Modifier) {
    LazyColumn {
        items(devices) { device ->
            DeviceItem(device, )
        }
    }
}

@Composable
fun DeviceItem(device: WifiP2pDevice,modifier: Modifier = Modifier) {
    Column {
        Text(text = "Device Name: ${device.deviceName}")
        Text(text = "Device Address: ${device.deviceAddress}")
    }
}



class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
//    private val activity: MainActivity
) : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel) { peers ->
                    // Handle discovered peers
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Handle connection state changes
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private val discoveredDevices = mutableStateListOf<WifiP2pDevice>()
//    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private val peerChangeReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) {
                // Request the current list of peers
                wifiP2pManager.requestPeers(channel) { peers ->
                    Log.d("WiFiP2P", "Peer list updated: $peers")
                    discoveredDevices.clear()
                    discoveredDevices.addAll(peers.deviceList)
                    // Update UI with the new peer list
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()

        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val pm = packageManager
        Log.d("WiFiDirect", "have wifi direct? ${pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)}")
        if (!pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Toast.makeText(this, "Wi-Fi Direct not supported on this device", Toast.LENGTH_LONG).show()
        }
        val channelListener = WifiP2pManager.ChannelListener {
                Log.d("WiFiDirect", "Channel disconnected. Reinitializing...")
        }
        channel = wifiP2pManager.initialize(this, mainLooper, channelListener)
        wifiP2pManager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WiFiP2P", "Stopped ongoing peer discovery.")
            }

            override fun onFailure(reason: Int) {
                Log.e("WiFiP2P", "Failed to stop peer discovery.")
            }
        })
        discoverDevices()


        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                    DeviceList(devices = discoveredDevices,
                        modifier = Modifier.padding(innerPadding)
                        )
                }
            }
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        discoverDevices()
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        registerReceiver(peerChangeReceiver, intentFilter)
    }
    override fun onPause() {
        super.onPause()
        unregisterReceiver(peerChangeReceiver)
    }
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        // Handle the result of the permission request
        permissions.entries.forEach { entry ->
            val permission = entry.key
            val isGranted = entry.value
            if (isGranted) {
                Log.d("Permissions", "$permission granted.")
            } else {
                Log.d("Permissions", "$permission denied.")
            }
        }
    }
    @SuppressLint("InlinedApi")
    private fun requestPermissions() {
        // Request FINE and COARSE location permissions
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                )
            )
    }
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    @SuppressLint("MissingPermission")
    private fun discoverDevices() {
        val permissionChecked = hasLocationPermissions()
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
//        startActivity(Intent(Settings.WIFI_P2P_PEERS_CHANGED_ACTION))
//        startActivity(Intent(Settings.EXTRA_WIFI_NETWORK_LIST))
        wifiP2pManager.requestGroupInfo(channel) { group ->
            if (group != null) {
                Log.d("WiFiP2P", "Wi-Fi Direct group is enabled.")
            } else {
                Log.e("WiFiP2P", "Wi-Fi Direct group is disabled.")
            }
        }
        if (permissionChecked && wifiManager.isWifiEnabled) {
            wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("WiFiDirect", "Dispositivos descubiertos correctamente.")
                }

                override fun onFailure(reason: Int) {
                    Log.e("WiFiDirect", "Error al buscar dispositivos: $reason")
                    Toast.makeText(
                        this@MainActivity,
                        "Error al buscar dispositivos.",
                        Toast.LENGTH_SHORT
                    ).show()
                    when (reason) {
                        WifiP2pManager.P2P_UNSUPPORTED -> Log.e("WiFiP2P", "Wi-Fi Direct is not supported on this device.")
                        WifiP2pManager.BUSY -> Log.e("WiFiP2P", "Wi-Fi is busy, try again later.")
                        WifiP2pManager.ERROR -> Log.e("WiFiP2P", "F An unexpected error occurred.")
                    }
                }
            })
            wifiP2pManager.requestGroupInfo(channel) { group ->
                Log.d("WiFiP2P", "Group Info: $group")
            }
            wifiP2pManager.requestPeers(channel) { peers ->
                Log.d("WiFiP2P", "Available peers: ${peers.deviceList}")
            }
            wifiP2pManager.requestConnectionInfo(channel) { info ->
                if (info != null) {
                    Log.d("WiFiP2P", "Group Owner Address: ${info}")
                } else {
                    Log.e("WiFiP2P", "Wi-Fi Direct connection info is unavailable.")
                }
            }
        }

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android",  modifier = Modifier.fillMaxSize())
    }
}
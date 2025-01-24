package com.example.myapplication

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
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
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    private lateinit var wifiDirectManager: WifiDirectManager
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)
        wifiDirectManager = WifiDirectManager(
            this,
            getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager,
            (getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager)
                .initialize(this, mainLooper, null)
        )

        permissionManager.requestPermissions()
        wifiDirectManager.discoverDevices()

        setContent {
            MyApplicationTheme {
                MainScreen(wifiDirectManager.discoveredDevices)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wifiDirectManager.discoverDevices()
        wifiDirectManager.registerReceiver()
    }

    override fun onPause() {
        super.onPause()
        wifiDirectManager.unregisterReceiver()
    }
}

@Composable
fun MainScreen(devices: List<WifiP2pDevice>) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        DeviceList(devices = devices, modifier = Modifier.padding(innerPadding))
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
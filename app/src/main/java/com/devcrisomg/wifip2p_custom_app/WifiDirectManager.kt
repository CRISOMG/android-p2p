package com.devcrisomg.wifip2p_custom_app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.compose.runtime.mutableStateListOf

class WifiDirectManager(
    private val context: Context,
    private val wifiP2pManager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel
) {
    val discoveredDevices = mutableStateListOf<WifiP2pDevice>()

    fun setDiscoveredDevices (peers: WifiP2pDeviceList) {
        Log.d("WiFiP2P", "Peer list updated: $peers")
        discoveredDevices.clear()
        discoveredDevices.addAll(peers.deviceList)
        // Update UI with the new peer list
    }
    @SuppressLint("MissingPermission")
    fun discoverDevices() {
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WiFiDirect", "Discovery started successfully.")
                wifiP2pManager.requestPeers(channel) { peers ->
                    setDiscoveredDevices(peers)
                }
            }

            override fun onFailure(reason: Int) {
                Log.e("WiFiDirect", "Discovery failed: $reason")
            }
        })
    }

    private val peerChangeReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) {
                // Request the current list of peers
                wifiP2pManager.requestPeers(channel) { peers ->
                    setDiscoveredDevices(peers)
                }
            }
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun registerReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(peerChangeReceiver, intentFilter)
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(peerChangeReceiver)
    }
}

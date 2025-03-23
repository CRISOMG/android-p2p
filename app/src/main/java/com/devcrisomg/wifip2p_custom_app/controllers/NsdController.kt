package com.devcrisomg.wifip2p_custom_app.controllers

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.devcrisomg.wifip2p_custom_app.components.DeviceInfoModel
import com.devcrisomg.wifip2p_custom_app.utils.GenericEventBus
import com.devcrisomg.wifip2p_custom_app.utils.getLocalIpAddress

class NsdController(
    private val context: Context,
    ) {
    @SuppressLint("NewApi")
    val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    val onDeviceResolved: GenericEventBus<DeviceInfoModel> = GenericEventBus()
    val discoveryListener = @SuppressLint("NewApi")
    object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            Log.d("NsdManager", "Descubrimiento iniciado")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.d("NsdManager", "Servicio encontrado: ${serviceInfo.serviceName}")
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e("NsdManager", "Error al resolver servicio: $errorCode")
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val ip = serviceInfo.hostAddresses[0].toString()
                    val name = serviceInfo.serviceName
                    val txtRecords = serviceInfo.attributes // Obtén los registros TXT
                    val deviceNameBytes = txtRecords["deviceName"]
                    val deviceName = deviceNameBytes?.let { String(it, Charsets.UTF_8) }
                    Log.d("NsdManager", "Servicio de ${deviceName} resuelto: ${serviceInfo.hostAddresses[0]}:${serviceInfo.port}")

                    onDeviceResolved.publish(DeviceInfoModel(
                        name= name,
                        ip = ip,
                        ip_lan = ip
                    ))
                }
            })
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.e("NsdManager", "Servicio perdido: ${serviceInfo.serviceName}")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d("NsdManager", "Descubrimiento detenido")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("NsdManager", "Error al iniciar descubrimiento: $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("NsdManager", "Error al detener descubrimiento: $errorCode")
        }
    }
    @SuppressLint("NewApi")
    fun startDiscovery() {
        nsdManager.discoverServices("_myomgservice._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }
    @SuppressLint("NewApi")
    fun stopDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }


    @SuppressLint("NewApi")
    fun advertiseService( ) {

        val deviceName = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME);
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "MyOMGAppService-Android" // Nombre de tu servicio
            serviceType = "_myomgservice._tcp" // Tipo de servicio
            this.port = 8888 // Puerto donde corre tu servicio
            setAttribute("tag","MyOMGAppServiceAndroid")
            setAttribute("appVersion","1.0")
            setAttribute("ip", getLocalIpAddress(context))
            setAttribute("port", "${8888}")
            setAttribute("deviceName", deviceName)
        }

        val registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d("NsdManager", "Servicio registrado: ${serviceInfo.serviceName}")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d("NsdManager", "Servicio eliminado")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdManager", "Error al registrar servicio: $errorCode")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdManager", "Error al eliminar servicio: $errorCode")
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private val handler = Handler(Looper.getMainLooper())

    private val announceRunnable = object : Runnable {
        override fun run() {
            advertiseService()
            handler.postDelayed(this, 60000) // Reanuncia cada 60 segundos
        }
    }

    fun startPeriodicAdvertising() {
        handler.post(announceRunnable)
    }

    fun stopPeriodicAdvertising() {
        handler.removeCallbacks(announceRunnable)
    }
}
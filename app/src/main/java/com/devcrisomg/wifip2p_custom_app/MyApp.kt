package com.devcrisomg.wifip2p_custom_app
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {
    @Inject
    lateinit var eventBus: DeviceEventBus

    override fun onCreate() {
        super.onCreate()
    }
}
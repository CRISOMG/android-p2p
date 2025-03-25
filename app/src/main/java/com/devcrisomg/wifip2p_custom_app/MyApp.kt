package com.devcrisomg.wifip2p_custom_app
import android.app.Application
import com.devcrisomg.wifip2p_custom_app.components.DeviceEventBus
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

//    @Inject
//    lateinit var eventBus: DeviceEventBus
//    companion object {
//        lateinit var instance: MyApp
//            private set
//    }
    override fun onCreate() {
        super.onCreate()
//        instance = this
    }
}
package com.devcrisomg.wifip2p_custom_app
import android.util.Log
import com.devcrisomg.wifip2p_custom_app.components.DeviceEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // El alcance es de toda la app
object AppModule {
//    @Provides
//    @Singleton
//    fun provideDeviceEventBus(): DeviceEventBus = DeviceEventBus()
//    fun provideDeviceEventBus(): DeviceEventBus {
//        val eventBus = DeviceEventBus()
//        Log.d("HiltDebug", "DeviceEventBus creado: $eventBus")
//        return eventBus
//    }
}

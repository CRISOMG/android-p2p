package com.devcrisomg.wifip2p_custom_app
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // El alcance es de toda la app
object AppModule {
    @Provides
    @Singleton
    fun provideDeviceEventBus(): DeviceEventBus = DeviceEventBus()
}

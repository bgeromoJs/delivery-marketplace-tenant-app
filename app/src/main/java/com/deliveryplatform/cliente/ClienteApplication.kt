package com.deliveryplatform.cliente

import android.app.Application
import com.deliveryplatform.cliente.data.AppContainer

class ClienteApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

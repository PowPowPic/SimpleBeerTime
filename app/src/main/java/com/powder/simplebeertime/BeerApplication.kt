package com.powder.simplebeertime

import android.app.Application
import com.powder.simplebeertime.di.AppContainer

class BeerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
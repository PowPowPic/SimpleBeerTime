package com.powder.simplebeertime

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.powder.simplebeertime.di.AppContainer
import com.powder.simplebeertime.util.AdManager

class BeerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        MobileAds.initialize(this) {}
        AdManager.loadAd(this)
    }
}

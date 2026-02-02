package ru.pavlig43.nocombroapp

import android.app.Application
import org.koin.android.ext.koin.androidContext
import ru.pavlig43.rootnocombro.internal.di.initKoin

class Nocombro : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@Nocombro)
        }
    }
}
package ru.pavlig43.nocombro

import android.app.Application
import org.koin.android.ext.koin.androidContext

class Nocombro : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@Nocombro)
        }
    }
}

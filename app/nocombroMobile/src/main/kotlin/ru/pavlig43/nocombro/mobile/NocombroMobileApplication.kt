package ru.pavlig43.nocombro.mobile

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.pavlig43.nocombro.mobile.internal.di.nocombroMobileModule

/**
 * Инициализирует app-level DI для Android-сборки.
 */
class NocombroMobileApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@NocombroMobileApplication)
            modules(nocombroMobileModule)
        }
    }
}

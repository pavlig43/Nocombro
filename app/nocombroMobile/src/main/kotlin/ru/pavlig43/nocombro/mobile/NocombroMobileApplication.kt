package ru.pavlig43.nocombro.mobile

import android.app.Application
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.conscrypt.Conscrypt
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.pavlig43.nocombro.mobile.internal.di.nocombroMobileModule

/**
 * Инициализирует Android-приложение, crypto providers и app-level DI.
 */
class NocombroMobileApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installBouncyCastleProvider()
        installConscryptProvider()

        startKoin {
            androidContext(this@NocombroMobileApplication)
            modules(nocombroMobileModule)
        }
    }

    /**
     * Добавляет Conscrypt для TLS после BouncyCastle, если его ещё нет.
     */
    private fun installConscryptProvider() {
        if (Security.getProvider(CONSCRYPT_PROVIDER_NAME) == null) {
            Security.insertProviderAt(Conscrypt.newProvider(), 2)
        }
    }

    /**
     * Ставит BouncyCastle первым, чтобы Android мог подписывать IAM JWT через PS256.
     */
    private fun installBouncyCastleProvider() {
        val existing = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        if (existing?.javaClass?.name != BouncyCastleProvider::class.java.name) {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
    }

    private companion object {
        const val CONSCRYPT_PROVIDER_NAME = "Conscrypt"
    }
}

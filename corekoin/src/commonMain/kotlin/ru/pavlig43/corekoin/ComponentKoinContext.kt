package ru.pavlig43.corekoin

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.koinApplication

@OptIn(KoinInternalApi::class)
class ComponentKoinContext : InstanceKeeper.Instance {
    private var koinApp: KoinApplication? = null

    fun getOrCreateKoinScope(modules: List<Module>): Scope {
        if (koinApp == null) {
            koinApp = koinApplication { modules(modules) }
        }
        return requireNotNull(koinApp).koin.scopeRegistry.rootScope
    }

    override fun onDestroy() {
        koinApp?.close()
        koinApp = null
    }
}

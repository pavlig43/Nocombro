package ru.pavlig43.signroot.internal.di

import org.koin.dsl.module
import ru.pavlig43.signroot.api.IRootSignDependencies

internal fun createRootSignModule(rootSignDependencies: IRootSignDependencies): List<Module> {
    return listOf(
        rootSignModule,
        module {
            factory { rootSignDependencies }
        }
    )
}
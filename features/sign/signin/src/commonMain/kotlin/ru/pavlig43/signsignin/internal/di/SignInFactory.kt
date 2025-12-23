package ru.pavlig43.signsignin.internal.di

import org.koin.dsl.module
import ru.pavlig43.signsignin.api.ISignInDependencies

internal fun createSignInModule(signInDependencies: ISignInDependencies): List<Module> {
    return listOf(
        signInModule,
        module {
            factory { signInDependencies }
        }
    )

}
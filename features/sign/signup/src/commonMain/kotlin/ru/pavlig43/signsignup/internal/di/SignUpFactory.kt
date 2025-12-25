package ru.pavlig43.signsignup.internal.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.pavlig43.signsignup.api.ISignUpDependencies

internal fun createSignUpModule(signUpDependencies: ISignUpDependencies): List<Module> {
    return listOf(
        signUpModule,
        module {
            factory { signUpDependencies }
        }
    )

}
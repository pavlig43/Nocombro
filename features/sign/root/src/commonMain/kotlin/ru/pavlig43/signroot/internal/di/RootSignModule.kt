package ru.pavlig43.signroot.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.signsignin.api.ISignInDependencies
import ru.pavlig43.signsignup.api.ISignUpDependencies


internal val rootSignModule = module {
    singleOf(::SignInDependencies) bind ISignInDependencies::class
    singleOf(::SignUpDependencies) bind ISignUpDependencies::class
}
/**
 * TODO()
 */
private class SignInDependencies : ISignInDependencies

/**
 * TODO()
 */
internal class SignUpDependencies : ISignUpDependencies
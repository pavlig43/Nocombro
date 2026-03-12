package ru.pavlig43.signsignup.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.signcommon.logopass.api.data.ILogoPassRepository
import ru.pavlig43.signsignup.internal.data.LogoPassSignUpRepository

internal val signUpModule = module {
    factoryOf(::LogoPassSignUpRepository) bind ILogoPassRepository::class

}


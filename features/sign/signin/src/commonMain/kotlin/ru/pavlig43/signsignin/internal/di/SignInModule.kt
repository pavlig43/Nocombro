package ru.pavlig43.signsignin.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.pavlig43.signcommon.logopass.api.data.ILogoPassRepository
import ru.pavlig43.signsignin.internal.data.LogoPassSignInRepository

internal val signInModule = module {
    factoryOf(::LogoPassSignInRepository) bind ILogoPassRepository::class
}


package ru.pavlig43.signsignin.internal.data

import ru.pavlig43.signcommon.logopass.api.data.LogoPass
import ru.pavlig43.signcommon.logopass.api.data.LogoPassResult
import ru.pavlig43.core.RequestResult
import ru.pavlig43.signcommon.logopass.api.data.ILogoPassRepository

/**
 * TODO("Сделать вход через бд")
 */
internal class LogoPassSignInRepository: ILogoPassRepository {
    override suspend fun sendLogoPass(logoPass: LogoPass): RequestResult<LogoPassResult> {
        return RequestResult.Success(LogoPassResult("token"))
    }
    private companion object {
        const val LOGO_PASS_SIGN_IN_REPOSITORY = "LOGO_PASS_SIGN_IN_REPOSITORY"
    }
}
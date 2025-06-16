package ru.pavlig43.signcommon.logopass.api.data

import ru.pavlig43.core.RequestResult

interface ILogoPassRepository {
    suspend fun sendLogoPass(logoPass: LogoPass):RequestResult<LogoPassResult>
}
package ru.pavlig43.signcommon.logopass.api.data


interface ILogoPassRepository {
    suspend fun sendLogoPass(logoPass: LogoPass):Result<LogoPassResult>
}
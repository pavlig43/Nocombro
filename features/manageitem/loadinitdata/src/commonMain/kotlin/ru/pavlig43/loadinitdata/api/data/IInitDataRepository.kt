package ru.pavlig43.loadinitdata.api.data

import ru.pavlig43.core.RequestResult


interface IInitDataRepository<I:Any,S:Any> {

    suspend fun loadInitData(id:Int):RequestResult<S>
    val iniDataForState:S


}




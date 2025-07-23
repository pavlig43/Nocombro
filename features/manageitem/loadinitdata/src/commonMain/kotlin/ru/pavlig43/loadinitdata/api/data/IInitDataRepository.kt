package ru.pavlig43.loadinitdata.api.data

import ru.pavlig43.core.RequestResult

interface IInitDataRepository<I:Any> {

    suspend fun loadInitData(id:Int):RequestResult<I>

    fun getInitDataForState():I

}
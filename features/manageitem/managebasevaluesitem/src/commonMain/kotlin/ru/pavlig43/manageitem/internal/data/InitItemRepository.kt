package ru.pavlig43.manageitem.internal.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.mapTo
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository
import ru.pavlig43.manageitem.api.data.RequireValues

class InitItemRepository<I:Item>(
    private val tag:String,
    private val loadData:suspend (Int)-> I,
    override val iniDataForState: RequireValues,

    ): IInitDataRepository<I, RequireValues> {
    override suspend fun loadInitData(id: Int): RequestResult<RequireValues> {
        if (id == 0) return RequestResult.Success(iniDataForState)
        return dbSafeCall(tag){
            loadData(id)
        }.mapTo { item:I-> item.toRequireValues() }
    }
    private fun I.toRequireValues(): RequireValues {
        return RequireValues(
            id = id,
            name = displayName,
            type = type,
            createdAt = createdAt
        )
    }

}


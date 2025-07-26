package ru.pavlig43.loadinitdata.api.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.mapTo

class InitItemRepository<I:Item,UIState:Any>(
    private val tag:String,
    private val loadData:suspend (Int)-> I,
    override val iniDataForState: UIState,
    private val mapper:I.()->UIState,

):IInitDataRepository<I,UIState> {
    override suspend fun loadInitData(id: Int): RequestResult<UIState> {
        if (id == 0) return RequestResult.Success(iniDataForState)
        return dbSafeCall(tag){
            loadData(id)
        }.mapTo { item:I-> item.mapper() }
    }

}


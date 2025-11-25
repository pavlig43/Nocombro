package ru.pavlig43.form.api.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.data.dbSafeCall

interface IUpdateRepository<I : Any,O:Any> {
    suspend fun getInit(id: Int): RequestResult<O>
    suspend fun update(changeSet: ChangeSet<I>)
}


class UpdateItemRepository<I : GenericItem>(
    private val tag: String,
    private val loadItem: suspend (Int) -> I,
    private val updateItem: suspend (I) -> Unit,
) : IUpdateRepository<I,I> {

    override suspend fun getInit(id: Int): RequestResult<I> {
        return dbSafeCall(tag) {
            loadItem(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<I>) {
        if (changeSet.old == changeSet.new) return
        updateItem(changeSet.new)
    }
}



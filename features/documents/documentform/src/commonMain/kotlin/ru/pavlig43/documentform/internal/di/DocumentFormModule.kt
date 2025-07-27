package ru.pavlig43.documentform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentWithFiles
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.documentform.internal.ui.INIT_BASE_VALUES
import ru.pavlig43.documentform.internal.ui.SAVE_REPOSITORY_TAG
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository
import ru.pavlig43.manageitem.internal.data.InitItemRepository
import ru.pavlig43.manageitem.api.data.RequireValues
import ru.pavlig43.upsertitem.data.ISaveItemRepository
import ru.pavlig43.upsertitem.data.SaveItemRepository


internal val createDocumentFormModule = module {
    single<ISaveItemRepository<DocumentWithFiles>> { getSaveRepository(get())}
    single<IInitDataRepository<Document,RequireValues>> { getInitRequireValuesRepository(get()) }
}
private fun getSaveRepository(
    documentDao: DocumentDao
): ISaveItemRepository<DocumentWithFiles> {
    return SaveItemRepository(
        isNameExist = documentDao::isNameExist,
        insertNewItem = documentDao::insertDocumentWithWithFiles,
        updateItem = documentDao::updateDocumentWithFiles,
        tag = SAVE_REPOSITORY_TAG
    )
}
private fun getInitRequireValuesRepository(
    documentDao: DocumentDao
): IInitDataRepository<Document,RequireValues> {
    return InitItemRepository<Document,>(
        tag = INIT_BASE_VALUES,
        iniDataForState = RequireValues(),
        loadData = documentDao::getDocument,

    )
}

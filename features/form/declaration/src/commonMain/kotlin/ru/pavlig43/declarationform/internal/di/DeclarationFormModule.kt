package ru.pavlig43.declarationform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.DeclarationFile
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.declarationform.api.DeclarationDependencies
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.update.data.UpdateEssentialsRepository
import ru.pavlig43.update.data.UpdateCollectionRepository

internal fun createDeclarationFormModule(dependencies: DeclarationDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.transaction }
        single<CreateEssentialsRepository<Declaration>> {  getCreateRepository(get())}
        single<UpdateEssentialsRepository<Declaration>> {  getUpdateRepository(get())}
        single<ItemListDependencies> {dependencies.itemListDependencies  }

        single<UpdateCollectionRepository<DeclarationFile, DeclarationFile>> {
            getFilesRepository(
                get()
            )
        }
    }

)

private fun getCreateRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Declaration> {
    val dao = db.declarationDao
    return CreateEssentialsRepository(
        tag = "Create Declaration Repository",
        isNameAllowed = dao::isNameAllowed,
        create = dao::create
    )
}
private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Declaration>{
    val dao = db.declarationDao
    return UpdateEssentialsRepository(
        tag = "UpdateEssentialsRepository",
        isNameAllowed = dao::isNameAllowed,
        loadItem = dao::getDeclaration,
        updateItem = dao::updateDeclaration
    )
}
private fun getFilesRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<DeclarationFile, DeclarationFile> {
    val fileDao = db.declarationFilesDao
    return UpdateCollectionRepository(
        tag = "Declaration FilesRepository",
        loadCollection = fileDao::getFiles,
        deleteCollection = fileDao::deleteFiles,
        upsertCollection = fileDao::upsertFiles
    )
}

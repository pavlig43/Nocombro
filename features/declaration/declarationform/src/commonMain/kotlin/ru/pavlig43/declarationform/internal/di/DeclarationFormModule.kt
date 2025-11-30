package ru.pavlig43.declarationform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.DeclarationFile
import ru.pavlig43.declarationform.api.DeclarationDependencies
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.manageitem.api.UpsertEssentialsDependencies
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository

internal fun createDeclarationFormModule(dependencies: DeclarationDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.transaction }
        single<UpsertEssentialsDependencies> { dependencies.upsertEssentialsDependencies }
        single<ItemListDependencies> {dependencies.itemListDependencies  }
//        single<UpdateItemRepository<DeclarationIn>> { getCreateRepository(get()) }
//        single<IUpdateRepository<DeclarationIn, DeclarationIn>>(
//            named(UpdateRepositoryType.Declaration.name)) {
//            getInitItemRepository(
//                get()
//            )
//        }
        single<UpdateCollectionRepository<DeclarationFile, DeclarationFile>> {
            getFilesRepository(
                get()
            )
        }
    }

)

//private fun getCreateRepository(
//    db: NocombroDatabase
//): UpdateItemRepository<DeclarationIn> {
//    val dao = db.declarationDao
//    return UpdateItemRepository(
//        tag = "Create Declaration Repository",
//        isNameAllowed = dao::isNameAllowed,
//        create = dao::create
//    )
//}
//
//internal enum class UpdateRepositoryType {
//    Declaration,
//}
//
//private fun getInitItemRepository(
//    db: NocombroDatabase
//): IUpdateRepository<DeclarationIn, DeclarationIn> {
//    val dao = db.declarationDao
//    return UpdateItemRepository<DeclarationIn>(
//        tag = "Update Declaration Repository",
//        loadItem = dao::getDeclaration,
//        updateItem = dao::updateDeclaration
//    )
//}
//
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

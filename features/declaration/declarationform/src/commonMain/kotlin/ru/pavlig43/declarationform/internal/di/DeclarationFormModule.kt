package ru.pavlig43.declarationform.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.DeclarationFile
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.form.api.data.UpdateItemRepository
import ru.pavlig43.manageitem.api.data.CreateItemRepository
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository

internal val declarationFormModule = module {

    single<CreateItemRepository<DeclarationIn>> { getCreateRepository(get()) }
    single<IUpdateRepository<DeclarationIn, DeclarationIn>>(named(UpdateRepositoryType.Declaration.name)) { getInitItemRepository(get()) }
    single<UpdateCollectionRepository<DeclarationFile, DeclarationFile>>{ getFilesRepository(get()) }
}
private fun getCreateRepository(
    db: NocombroDatabase
): CreateItemRepository<DeclarationIn> {
    val dao = db.declarationDao
    return CreateItemRepository(
        tag = "Create Declaration Repository",
        isNameAllowed = dao::isNameAllowed,
        create = dao::create
    )
}

internal enum class UpdateRepositoryType{
    Declaration,
}
private fun getInitItemRepository(
    db: NocombroDatabase
): IUpdateRepository<DeclarationIn, DeclarationIn> {
    val dao = db.declarationDao
    return UpdateItemRepository<DeclarationIn>(
        tag = "Update Declaration Repository",
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

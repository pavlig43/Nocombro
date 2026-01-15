package ru.pavlig43.declaration.internal.di

import org.koin.dsl.module
import ru.pavlig43.addfile.api.FilesDependencies
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.declaration.api.DeclarationFormDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal fun createDeclarationFormModule(dependencies: DeclarationFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.transaction }
        single<ImmutableTableDependencies> {dependencies.immutableTableDependencies  }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<CreateEssentialsRepository<Declaration>> {  getCreateRepository(get())}
        single<UpdateEssentialsRepository<Declaration>> {  getUpdateRepository(get())}

    }

)

private fun getCreateRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Declaration> {
    val dao = db.declarationDao
    return CreateEssentialsRepository(
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}
private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Declaration>{
    val dao = db.declarationDao
    return UpdateEssentialsRepository(
        isCanSave = dao::isCanSave,
        loadItem = dao::getDeclaration,
        updateItem = dao::updateDeclaration
    )
}


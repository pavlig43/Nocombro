package ru.pavlig43.declaration.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.declaration.api.DeclarationFormDependencies
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository

internal fun createDeclarationFormModule(dependencies: DeclarationFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<ImmutableTableDependencies> {dependencies.immutableTableDependencies  }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<CreateSingleItemRepository<Declaration>> {  getCreateRepository(get())}
        single<UpdateSingleLineRepository<Declaration>> {  DeclarationUpdateRepository(get())}

    }

)

private fun getCreateRepository(
    db: NocombroDatabase
): CreateSingleItemRepository<Declaration> {
    val dao = db.declarationDao
    return CreateSingleItemRepository(
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}
private class DeclarationUpdateRepository(
    private val db: NocombroDatabase
) : UpdateSingleLineRepository<Declaration> {

    private val dao = db.declarationDao

    override suspend fun getInit(id: Int): Result<Declaration> {
        return runCatching {
            dao.getDeclaration(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<Declaration>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)
        return runCatching {
            dao.isCanSave(changeSet.new).getOrThrow()
            dao.updateDeclaration(changeSet.new)
        }
    }
}



package ru.pavlig43.files.internal.data

import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.UpsertListChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType

internal class FilesRepository(
    db: NocombroDatabase,
)  {
    private val dao = db.fileDao
    suspend fun getInit(ownerId: Int,ownerType: OwnerType): Result<List<FileBD>> {
        return runCatching {
            dao.getFiles(ownerId,ownerType)
        }
    }

    suspend fun update(changeSet: ChangeSet<List<FileBD>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = dao::deleteFiles,
            upsert = dao::upsertFiles
        )

    }

}
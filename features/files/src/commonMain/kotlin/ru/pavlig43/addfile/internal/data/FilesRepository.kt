package ru.pavlig43.addfile.internal.data

import ru.pavlig43.core.data.ChangeSet
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
        return runCatching {
            val (old, new) = changeSet
            val newById = new.associateBy { it.id }
            if (old != null) {
                val oldById = old.associateBy { it.id }
                val idsForDelete = oldById.keys - newById.keys
                dao.deleteFiles(idsForDelete.toList())
                val collectionForUpsert = new.filter { newItem ->
                    val oldItem = oldById[newItem.id]
                    oldItem == null || oldItem != newItem
                }
                dao.upsertFiles(collectionForUpsert)
            } else {
                dao.upsertFiles(new)
            }
        }

    }

}
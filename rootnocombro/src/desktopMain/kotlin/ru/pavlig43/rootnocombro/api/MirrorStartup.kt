package ru.pavlig43.rootnocombro.api

import ru.pavlig43.database.data.sync.mirror.MirrorStartupMaintenance

suspend fun runMirrorStartupMaintenance(rootDependencies: RootDependencies): String? {
    val result = MirrorStartupMaintenance(rootDependencies.database).cleanupSoftDeletedRows()
    if (result.deletedRows == 0) return null
    return "Mirror startup cleanup: tombstones=${result.transferredTombstones}, " +
        "rows=${result.deletedRows}"
}

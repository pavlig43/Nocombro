package ru.pavlig43.files.api

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway

class FilesDependencies(
    val db: NocombroDatabase,
    val remoteFileStorageGateway: RemoteFileStorageGateway,
)

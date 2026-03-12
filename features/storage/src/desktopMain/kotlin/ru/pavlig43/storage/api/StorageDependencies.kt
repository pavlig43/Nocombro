package ru.pavlig43.storage.api

import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.NocombroDatabase

class StorageDependencies(
    val db: NocombroDatabase,
    val tabOpener: TabOpener
)
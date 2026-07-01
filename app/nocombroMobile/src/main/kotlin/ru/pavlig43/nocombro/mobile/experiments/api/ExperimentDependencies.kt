package ru.pavlig43.nocombro.mobile.experiments.api

import ru.pavlig43.nocombro.mobile.internal.database.NocombroMobileDatabase

class ExperimentDependencies(
    val database: NocombroMobileDatabase,
    val filesDirPath: String,
    val fileProviderAuthority: String,
)

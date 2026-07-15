package ru.pavlig43.files.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.files.remote.RemoteFileStorageGateway
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.files.api.PendingUploadRegistry
import ru.pavlig43.files.internal.data.FilesRepository

/**
 * Собирает Koin-модуль файлов с единым процессным реестром pending-загрузок.
 *
 * Один экземпляр [PendingUploadRegistry] делят все файловые репозитории scope,
 * чтобы повторные попытки и завершение загрузки видели общий снимок.
 */
internal fun filesModule(dependencies: FilesDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<RemoteFileStorageGateway> { dependencies.remoteFileStorageGateway }
        single { PendingUploadRegistry() }
        singleOf(::FilesRepository)
    },
)

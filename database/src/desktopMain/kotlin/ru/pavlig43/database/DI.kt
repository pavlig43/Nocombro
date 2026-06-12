package ru.pavlig43.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.pavlig43.database.data.sync.SyncStateEntity
import java.io.File

fun platformDataBaseModule(): Module = module {
    single<NocombroDatabase> { getNocombroDatabase() }

}

fun getNocombroDatabase(): NocombroDatabase {
    val appDataDir = getAppDataDirectory()
    val dbFile = File(appDataDir, "nocombro.db")

    val builder =  Room.databaseBuilder<NocombroDatabase>(
        name = dbFile.absolutePath,
    )
    val database = builder
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .addMigrations(MIGRATION_4_5)
        .addMigrations(MIGRATION_5_6)
        .addMigrations(MIGRATION_6_7)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    CoroutineScope(Dispatchers.IO).launch {
        val existingState = database.syncStateDao.getSyncState()
        database.syncStateDao.upsertSyncState(
            syncState = createInitialSyncState(existingState)
        )
    }
    return database
}

/**
 * Возвращает директорию приложения в профиле пользователя.
 *
 * На Windows сначала используем `%APPDATA%`, чтобы база жила в нормальном пользовательском
 * каталоге, а не во временной папке. Если переменная окружения недоступна, используем home-dir
 * как безопасный запасной вариант.
 */
private fun getAppDataDirectory(): File {
    val baseDir = System.getenv("APPDATA")
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?: File(System.getProperty("user.home"))
    return File(baseDir, "Nocombro").apply { mkdirs() }
}

/**
 * Собирает начальное состояние синхронизации для локальной базы.
 *
 * При повторном запуске сохраняем уже известные отметки pull/push.
 */
private fun createInitialSyncState(
    existingState: SyncStateEntity?,
) = SyncStateEntity(
    lastPullAt = existingState?.lastPullAt,
    lastPushAt = existingState?.lastPushAt,
)

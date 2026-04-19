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
import java.util.UUID

fun platformDataBaseModule(): Module = module {
    single<NocombroDatabase> { getNocombroDatabase() }

}

fun getNocombroDatabase(): NocombroDatabase {
    val appDataDir = getAppDataDirectory()
    val dbFile = File(appDataDir, "nocombro.db")
    val deviceId = getOrCreateDeviceId(appDataDir)

    val builder =  Room.databaseBuilder<NocombroDatabase>(
        name = dbFile.absolutePath,
    )
    val database = builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    CoroutineScope(Dispatchers.IO).launch {
        val existingState = database.syncDao.getSyncState()
        database.syncDao.upsertSyncState(
            syncState = createInitialSyncState(
                deviceId = deviceId,
                existingState = existingState,
            )
        )
        seedDatabase(database)
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
 * Возвращает постоянный идентификатор текущей установки приложения.
 *
 * Этот `deviceId` нужен для будущей синхронизации: по нему можно отличать изменения,
 * пришедшие с разных компьютеров. Идентификатор создается один раз и сохраняется рядом с БД.
 */
private fun getOrCreateDeviceId(appDataDir: File): String {
    val deviceIdFile = File(appDataDir, "device.id")
    if (deviceIdFile.exists()) {
        return deviceIdFile.readText().trim()
    }

    val deviceId = UUID.randomUUID().toString()
    deviceIdFile.writeText(deviceId)
    return deviceId
}

/**
 * Собирает начальное состояние синхронизации для локальной базы.
 *
 * При повторном запуске сохраняем уже известные отметки pull/push и курсор,
 * но актуализируем `deviceId`, который используется этой установкой приложения.
 */
private fun createInitialSyncState(
    deviceId: String,
    existingState: SyncStateEntity?,
) = SyncStateEntity(
    deviceId = deviceId,
    lastPullAt = existingState?.lastPullAt,
    lastPushAt = existingState?.lastPushAt,
    lastRemoteCursor = existingState?.lastRemoteCursor,
)

package ru.pavlig43.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * Вспомогательный объект для создания тестовых экземпляров базы данных.
 */
object DatabaseTestHelper {

    /**
     * Создаёт файловую базу данных для тестирования.
     *
     * В отличие от in-memory, файловая база persists на диске во временной директории.
     * Полезно для отладки тестов.
     *
     * @param name Имя базы данных
     * @return Builder для создания базы данных
     */
    fun createTempDatabaseBuilder(name: String = "test_db"): RoomDatabase.Builder<NocombroDatabase> {
        val tempDir = System.getProperty("java.io.tmpdir")
        val dbFile = File(tempDir, "${name}_${System.currentTimeMillis()}.db")

        // Удаляем файл если существует
        if (dbFile.exists()) {
            dbFile.delete()
        }

        return Room.databaseBuilder<NocombroDatabase>(
            name = dbFile.absolutePath,
        )
    }
}

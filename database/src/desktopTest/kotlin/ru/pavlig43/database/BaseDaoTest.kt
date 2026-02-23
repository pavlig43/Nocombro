package ru.pavlig43.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)

/**
 * Базовый класс для DAO тестов.
 *
 * Предоставляет:
 * - Тестовый диспетчер корутин
 * - Автоматическое создание и закрытие базы данных
 * - Базовые операции для работы с БД в тестах
 *
 * Примечание: Каждый тест должен создавать свою базу данных используя
 * createTestDatabase() для изоляции тестов друг от друга.
 */
abstract class BaseDaoTest : FunSpec({

    // Тестовый диспетчер корутин устанавливается перед каждым тестом
    beforeTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    // Диспетчер сбрасывается после каждого теста
    afterTest {
        Dispatchers.resetMain()
    }
})

/**
 * Создаёт новую временную базу данных для тестирования.
 *
 * Используйте эту функцию внутри каждого теста для создания изолированной БД.
 *
 * @return Новый экземпляр NocombroDatabase
 */
fun createTestDatabase(): NocombroDatabase {
    return DatabaseTestHelper.createTempDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Main)
        .build()
}

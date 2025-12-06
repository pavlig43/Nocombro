package ru.pavlig43.database.db

import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import ru.pavlig43.database.NocombroDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Базовый класс для unit-тестов всех DAO с in-memory Room базой.
 *
 * **Создает чистую БД для каждого теста** в RAM (исчезает после [tearDown]).
 * Поддерживает coroutines testing с [StandardTestDispatcher] и [kotlinx.coroutines.test.runTest].
 *
 * **Наследование:**
 * ```
 * class DocumentDaoTest : BaseDatabaseTest() {
 *     private lateinit var documentDao: DocumentDao
 *
 *     @BeforeTest
 *     fun daoSetup() {
 *         documentDao = database.documentDao  // без ()
 *     }
 * }
 * ```
 *
 * **Запуск:** `./gradlew commonTest`
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseDatabaseTest {

    /**
     * In-memory тестовая база Room. **НЕ сохраняется** на диск.
     * Доступны все DAO через `database.documentDao` (без `()`).
     */
    protected lateinit var database: NocombroDatabase

    /**
     * Test-диспетчер для контроля корутин в тестах.
     * Используется в `runTest(testDispatcher)` и `advanceUntilIdle()`.
     */
    protected val testDispatcher = StandardTestDispatcher()

    /**
     * Создает чистую in-memory БД перед каждым тестом.
     *
     * - `.fallbackToDestructiveMigration(true)` — игнорирует миграции
     * - Каждый тест получает **пустую схему**
     */
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder<NocombroDatabase>()
            .setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
    }

    /**
     * Закрывает БД и сбрасывает диспетчер после каждого теста.
     * **БД удаляется из памяти навсегда.**
     */
    @AfterTest
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }
}

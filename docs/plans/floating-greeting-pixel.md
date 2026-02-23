# План внедрения тестирования в Nocombro

## Обзор текущего состояния

- **Единственный тест**: `core/src/commonTest/kotlin/ru/pavlig43/core/SampleTest.kt`
- **TestingPlugin** настроен и добавляет зависимости: Kotest, MockK, Turbine, Coroutines Test
- **Покрытие**: ~0% — нет тестов в critical модулях (database, features, business logic)

## Стартовый приоритет: Database слой

### Задача 1: Создать тесты для DAO

**Файлы для создания:**

```
database/src/commonTest/kotlin/ru/pavlig43/database/dao/
├── ProductDaoTest.kt
├── VendorDaoTest.kt
├── DeclarationDaoTest.kt
├── BatchDaoTest.kt
└── TransactionDaoTest.kt
```

**Шаблон теста (ProductDaoTest.kt):**
```kotlin
import ru.pavlig43.database.dao.ProductDao
import ru.pavlig43.database.entity.ProductEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ProductDaoTest : FunSpec({
    // TODO: Настроить in-memory Room БД
    // TODO: Написать тесты: insert, update, delete, getById, getAll
})
```

---

### Задача 2: Создать Test Fixtures модуль

**Создать:** `core/test-fixtures/commonMain/kotlin/ru/pavlig43/testfixtures/`

```kotlin
// TestData.kt
object TestData {
    val testVendor = VendorEntity(id = 1, name = "Test Vendor")

    fun createTestProduct(id: Int = 1) = ProductEntity(
        id = id,
        name = "Test Product $id",
        // ... остальные поля
    )
}

// DatabaseTestHelper.kt
object DatabaseTestHelper {
    fun createInMemoryDatabase(): NocombroDatabase {
        // TODO: Реализовать создание in-memory БД
    }
}
```

---

### Задача 3: Тесты для связей @Relation

**Файл:** `database/src/commonTest/.../EntityRelationsTest.kt`

Тестирование:
- Product ↔ Declaration (M:N)
- Product → Batch (1:N)
- Batch → BatchMovement (1:N)

---

## Критические файлы для модификации

| Файл | Изменение |
|------|-----------|
| `database/build.gradle.kts` | Добавить `commonTest` source set (если нет) |
| `build.gradle.kts (root)` | Добавить Kover plugin для покрытия |
| `core/build.gradle.kts` | Создать `test-fixtures` source set |

---

## Проверка (Verification)

1. Запустить тесты: `./gradlew :database:allTests`
2. Проверить покрытие: `./gradlew koverHtmlReport`
3. Открыть отчёт: `build/reports/kover/html/index.html`

---

## Следующие шаги (после старта)

1. Тесты для бизнес-логики транзакций
2. Тесты для Decompose Components
3. Compose UI тесты
4. Настройка CI для автозапуска тестов

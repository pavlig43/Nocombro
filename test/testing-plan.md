# План внедрения тестирования в Nocombro

## Текущий статус

**Дата:** 23 февраля 2026
**Статус:** ✅ Базовый слой DAO протестирован

## Что сделано

### 1. Инфраструктура ✅
- `database/build.gradle.kts` - добавлен `TestingPlugin`
- `database/src/desktopTest/` - создана source set для тестов
- Kotest + Turbine + MockK настроены через `TestingPlugin`

### 2. Хелперы ✅
- `DatabaseTestHelper.kt` - создание временной БД для тестов
- `TestEntities.kt` - фабрики тестовых сущностей
- `createTestDatabase()` - функция для создания изолированной БД

### 3. DAO тесты ✅
| DAO | Тесты | Статус |
|-----|-------|--------|
| `VendorDaoTest` | 4 | ✅ PASSED |
| `ProductDaoTest` | 3 | ✅ PASSED |
| `DeclarationDaoTest` | 3 | ✅ PASSED |
| `BatchDaoTest` | 1 | ✅ PASSED |
| **ИТОГО** | **11** | **✅** |

## Запуск тестов

```bash
# Все database тесты
./gradlew :database:desktopTest

# Конкретный тест
./gradlew :database:desktopTest --tests "VendorDaoTest"

# Отчёт
# database/build/reports/tests/desktopTest/index.html
```

## Следующие шаги

### Приоритет 1: Business Logic
- [ ] Тесты для `TransactionDao`
- [ ] Тесты для `BuyDao`, `SaleDao`
- [ ] Тесты для `BatchMovementDao` (нужен Transact)

### Приоритет 2: Component Layer
- [ ] Тесты для Decompose Components
- [ ] Тесты для UseCases
- [ ] Тесты для ViewModels

### Приоритет 3: UI Tests
- [ ] Compose UI тесты (androidInstrumentedTest)
- [ ] Снимки UI для регрессионного тестирования

### Приоритет 4: CI/CD
- [ ] Автозапуск тестов на CI
- [ ] Кодовое покрытие (Kover)
- [ ] SonarQube integration

## Шаблон DAO теста

```kotlin
class SomeDaoTest : FunSpec({

    lateinit var database: NocombroDatabase
    lateinit var someDao: SomeDao

    beforeTest {
        database = createTestDatabase()
        someDao = database.someDao
    }

    afterTest {
        database.close()
    }

    test("should do something") {
        // Arrange
        val entity = TestEntities.createTestEntity(id = 0)

        // Act
        val result = someDao.create(entity)

        // Assert
        result shouldBe 1L
    }
})
```

## Важные правила

1. **FK Constraints** - создавай родительские сущности перед дочерними
2. **Isolation** - каждый тест создаёт новую БД (through `createTestDatabase()`)
3. **Turbine** - для тестирования Flow используй `turbine.test { }`
4. **Imports** - используй `io.kotest.matchers.shouldBe`, не wildcard

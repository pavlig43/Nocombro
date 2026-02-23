# Грабли (уроки) при написании тестов для Nocombro

## Room Database на KMP

### ❌ НЕ ДЕЛАЙ ТАК: `:memory:` база данных
```kotlin
// НЕ РАБОТАЕТ!
Room.databaseBuilder<NocombroDatabase>(
    name = ":memory:",  // IllegalArgumentException!
)
```

**Ошибка:** `Cannot build a database with the special name ':memory:'.`

### ✅ ДЕЛАЙ ТАК: Временная файловая база
```kotlin
fun createTempDatabaseBuilder(name: String = "test_db"): RoomDatabase.Builder<NocombroDatabase> {
    val tempDir = System.getProperty("java.io.tmpdir")
    val dbFile = File(tempDir, "${name}_${System.currentTimeMillis()}.db")

    return Room.databaseBuilder<NocombroDatabase>(
        name = dbFile.absolutePath,
    )
}
```

---

## Kotest FunSpec lifecycle

### ❌ НЕ ДЕЛАЙ ТАК: override методов с TestResult
```kotlin
abstract class BaseDaoTest : FunSpec() {
    // НЕ РАБОТАЕТ в Kotest 6.x!
    override suspend fun beforeTest(testCase: TestCase) { }
    override suspend fun afterTest(testCase: TestCase, result: TestResult) { }
}
```

**Ошибка:** `Unresolved reference 'TestResult'`, `'afterTest' overrides nothing`

### ✅ ДЕЛАЙ ТАК: Используй lambda body
```kotlin
abstract class BaseDaoTest : FunSpec({
    beforeTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    afterTest {
        Dispatchers.resetMain()
    }
})
```

---

## lateinit var внутри FunSpec {}

### ❌ НЕ ДЕЛАЙ ТАК: val с getter
```kotlin
class VendorDaoTest : FunSpec({
    val vendorDao: VendorDao
        get() = database.vendorDao  // Variable 'vendorDao' must be initialized
})
```

### ✅ ДЕЛАЙ ТАК: lateinit var + инициализация в beforeTest
```kotlin
class VendorDaoTest : FunSpec({
    lateinit var database: NocombroDatabase
    lateinit var vendorDao: VendorDao

    beforeTest {
        database = createTestDatabase()
        vendorDao = database.vendorDao
    }
})
```

---

## Foreign Keys в тестах

### ❌ НЕ ДЕЛАЙ ТАК: Забудь про FK
```kotlin
// Declaration имеет FK на Vendor!
val declaration = TestEntities.createTestDeclaration(id = 0, vendorId = 1)
declarationDao.create(declaration)  // FK constraint failed!
```

### ✅ ДЕЛАЙ ТАК: Создавай родителя первым
```kotlin
val vendor = TestEntities.createTestVendor(id = 0)
database.vendorDao.create(vendor)

val declaration = TestEntities.createTestDeclaration(
    id = 0,
    vendorId = 1,  // Ссылается на созданного vendor
    vendorName = "Test Vendor"
)
declarationDao.create(declaration)  // ✅
```

---

## Kotest assertions import

### ❌ НЕ ДЕЛАЙ ТАК
```kotlin
import ru.pavlig43.kotest.assertions.shouldBe  // Не существует!
```

### ✅ ДЕЛАЙ ТАК
```kotlin
import io.kotest.matchers.shouldBe
```

---

## Entity поля

### ProductType
```kotlin
// НЕ СУЩЕСТВУЕТ!
ProductType.PRODUCT
ProductType.INGREDIENT

// ПРАВИЛЬНО
ProductType.FOOD_BASE
ProductType.FOOD_PF
ProductType.PACK
```

### Declaration поля
```kotlin
// Обязательные поля!
Declaration(
    displayName: String,
    createdAt: LocalDate,
    vendorId: Int,
    vendorName: String,  // Не FK, просто строка!
    bornDate: LocalDate,
    bestBefore: LocalDate,
    observeFromNotification: Boolean,
    id: Int
)
```

### BatchMovement поля
```kotlin
BatchMovement(
    batchId: Int,
    movementType: MovementType,  // INCOMING/OUTGOING
    count: Int,
    transactionId: Int,  // FK на Transact!
    id: Int
)
```

---

## BatchMovement FK проблему

**Проблема:** BatchMovement имеет FK на `Transact`, но создание Transact в тестах сложное (нужен TransactionType, LocalDateTime и т.д.)

**Решение:** Упрости тесты - тестируй только BatchDao без BatchMovement

---

## Отладка тестов

1. **Логи:** Тесты выводят stacktrace сразу в консоль
2. **HTML отчёт:** `database/build/reports/tests/desktopTest/index.html`
3. **Pattern:** Создавай максимально простые тесты для начала

---

## Полезные команды

```bash
# Запуск конкретного теста
./gradlew :database:desktopTest --tests "VendorDaoTest.create should insert vendor"

# Пересобрать без кеша
./gradlew :database:clean :database:desktopTest

# Только compile (без запуска)
./gradlew :database:compileTestKotlinDesktop
```

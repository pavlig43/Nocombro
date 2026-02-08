# План внедрения тестирования в Nocombro (KMP)

## Выбранный подход
- **Уровень**: Полный (Unit + Integration + UI)
- **Фреймворки**: Kotest + Kotest Assertions + MockK
- **БД тесты**: Room inMemory
- **UI тесты**: Compose UI Testing + Decompose Testing

---

## Этап 1: Базовая инфраструктура (1-2 дня)

### 1.1 Создать convention плагин для тестов

**Файл**: `build-logic/convention/src/main/java/TestingPlugin.kt`

```kotlin
class TestingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Kotlin Multiplatform test setup
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    val commonTest by getting {
                        dependencies {
                            implementation(kotlin("test"))
                            implementation(libs.findLibrary("kotest").get())
                            implementation(libs.findLibrary("kotestAssertions").get())
                            implementation(libs.findLibrary("mockk").get())
                            implementation(libs.findLibrary("turbine").get())
                            implementation(libs.findLibrary("kotlinxCoroutinesTest").get())
                        }
                    }
                    val jvmTest by getting {
                        dependencies {
                            implementation(libs.findLibrary("kotestJunitRunner").get())
                        }
                    }
                }
            }
        }
    }
}
```

**Задачи**:
1. Создать `TestingPlugin.kt`
2. Добавить в `build-logic/convention/build.gradle.kts` зависимости Kotest, MockK
3. Зарегистрировать плагин в `settings.gradle.kts` как `pavlig43.testing`

### 1.2 Добавить зависимости в `gradle/libs.versions.toml`

```toml
[versions]
kotest = "5.9.1"
mockk = "1.13.12"

[libraries]
# Kotest
kotest = { module = "io.kotest:kotest-framework-engine", version.ref = "kotest" }
kotestAssertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
kotestJunitRunner = { module = "io.kotest:kotest-runner-junit5", version.ref = "kotest" }

# Mocking
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }

# Room testing
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

# Compose UI Testing
compose-ui-test = { group = "androidx.compose.ui", name = "ui-test-junit4", version.ref = "composeMultiplatform" }
compose-ui-testManifest = { group = "androidx.compose.ui", name = "ui-test-manifest", version.ref = "composeMultiplatform" }

[plugins]
pavlig43.testing = { id = "pavlig43.testing", version = "unspecified" }
```

### 1.3 Создать базовые test utilities

**Файл**: `core/testing/src/commonMain/kotlin/ru/pavlig43/testing/`

```kotlin
// BaseTest.kt
abstract class BaseTest {
    val testCoroutineScope = TestScope()

    fun runTest(block: suspend TestScope.() -> Unit) =
        testCoroutineScope.runTest(block)
}

// DatabaseTest.kt (для jvmTest)
abstract class DatabaseTest {
    fun createInMemoryDatabase(): AppDatabase =
        Room.inMemoryDatabaseBuilder()
            .build()
}
```

---

## Этап 2: Unit тесты (2-3 дня)

### 2.1 Тестирование бизнес-логики (core/)

**Файлы для создания**:

`core/src/commonTest/kotlin/ru/pavlig43/core/`

1. **CoroutineUtilsTest.kt**
```kotlin
class CoroutineUtilsTest : FunSpec({
    test("forEachParallel executes all blocks") {
        // Arrange
        val list = listOf(1, 2, 3)
        val results = mutableListOf<Int>()

        // Act
        runTest {
            list.forEachParallel {
                results.add(it)
            }
        }

        // Assert
        results shouldBe listOf(1, 2, 3)
    }
})
```

2. **DateTimeUtilsTest.kt** - тесты для работы с датами
3. **ValueToStateFlowTest.kt** - тесты трансформации состояний

### 2.2 Тестирование ViewModel (features/)

**Пример**: `features/sampletable/src/commonTest/kotlin/ru/pavlig43/sampletable/SampleViewModelTest.kt`

```kotlin
class SampleViewModelTest : FunSpec({
    // Mock dependencies
    val mockRepository = mockk<SampleRepository>()

    test("when loadItems, should update state to Loading") {
        // Arrange
        val viewModel = SampleViewModel(mockRepository)
        coEvery { mockRepository.getItems() } returns flowOf(emptyList())

        // Act
        viewModel.loadItems()

        // Assert
        viewModel.state.value shouldBe SampleState.Loading
    }
})
```

### 2.3 Тестирование фильтров и сортировки

`features/sampletable/src/commonTest/kotlin/ru/pavlig43/sampletable/filtering/`

1. **PersonFilterMatcherTest.kt**
2. **PersonSorterTest.kt**
3. **PersonValidatorTest.kt**

---

## Этап 3: Database тесты (2-3 дня)

### 3.1 Room DAO тесты

**Файл**: `database/src/jvmTest/kotlin/ru/pavlig43/database/dao/`

**ProductDaoTest.kt**:
```kotlin
class ProductDaoTest : FunSpec({
    lateinit var database: AppDatabase
    lateinit var dao: ProductDao

    beforeTest {
        database = Room.inMemoryDatabaseBuilder().build()
        dao = database.productDao()
    }

    afterTest {
        database.close()
    }

    test("insert product should return valid row id") {
        // Arrange
        val product = ProductEntity(
            id = 1,
            name = "Test Product"
        )

        // Act
        val rowId = dao.insert(product)

        // Assert
        rowId shouldBeGreaterThan 0L
    }

    test("get product by id should return correct product") {
        // Arrange & Act
        val product = ProductEntity(id = 1, name = "Test")
        dao.insert(product)
        val retrieved = dao.getById(1)

        // Assert
        retrieved shouldBe product
    }
})
```

### 3.2 Тесты TransactionExecutor

**Файл**: `database/src/jvmTest/kotlin/ru/pavlig43/database/TransactionExecutorTest.kt`

### 3.3 Тесты Type Converters

**Файл**: `database/src/jvmTest/kotlin/ru/pavlig43/database/converters/`

---

## Этап 4: Integration тесты (2-3 дня)

### 4.1 Repository тесты

**Файл**: `core/src/jvmTest/kotlin/ru/pavlig43/core/repository/`

```kotlin
class ProductRepositoryTest : FunSpec({
    lateinit var repository: ProductRepository
    lateinit var database: AppDatabase

    beforeTest {
        database = createInMemoryDatabase()
        repository = ProductRepository(database.productDao())
    }

    test("getProducts should return flow of products") {
        // Arrange
        val product = ProductEntity(id = 1, name = "Test")
        database.productDao().insert(product)

        // Act
        val products = repository.getProducts().testIn(testCoroutineScope)

        // Assert
        products.awaitItem() shouldBe listOf(product)
        products.cancel()
    }
})
```

### 4.2 Koin DI тесты

**Файл**: `corekoin/src/jvmTest/kotlin/ru/pavlig43/corekoin/KoinModuleTest.kt`

```kotlin
class KoinModuleTest : FunSpec({
    test("should provide all required dependencies") {
        // Start Koin
        startKoin { modules(appModule) }

        // Test injections
        val productDao: ProductDao = get()
        val repository: ProductRepository = get()

        productDao shouldNotBe null
        repository shouldNotBe null

        // Stop Koin
        stopKoin()
    }
})
```

---

## Этап 5: UI тесты (3-4 дня)

### 5.1 Compose UI тесты

**Файл**: `coreui/src/androidInstrumentedTest/kotlin/ru/pavlig43/coreui/components/`

```kotlin
class ButtonComponentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenButtonClicked_shouldInvokeOnClick() {
        var clicked = false

        composeTestRule.setContent {
            AppButton(onClick = { clicked = true }) {
                Text("Click me")
            }
        }

        composeTestRule.onNodeWithText("Click me")
            .performClick()

        assertTrue(clicked)
    }
}
```

### 5.2 ViewModel + UI тесты

**Файл**: `features/sampletable/src/androidInstrumentedTest/kotlin/ru/pavlig43/sampletable/SampleScreenTest.kt`

```kotlin
class SampleScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenItemsLoaded_shouldDisplayItems() {
        val viewModel = SampleViewModel(mockRepository)

        composeTestRule.setContent {
            SampleScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Item 1")
            .assertIsDisplayed()
    }
}
```

### 5.3 Decompose Navigation тесты

**Файл**: `core/src/commonTest/kotlin/ru/pavlig43/core/navigation/`

```kotlin
class TabNavigationComponentTest : FunSpec({
    test("when switching tabs, should update selected index") {
        val component = TestTabNavigationComponent()

        component.onTabClicked(1)

        component.state.value.selectedIndex shouldBe 1
    }
})
```

---

## Этап 6: CI/CD настройка (1 день)

### 6.1 Обновить `.github/workflows/detekt.yml`

Добавить job для тестов:

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Run Unit Tests
        run: ./gradlew test

      - name: Run Integration Tests
        run: ./gradlew jvmTest

      - name: Generate Test Report
        uses: dorny/test-reporter@v1
        with:
          name: Test Results
          path: '**/build/test-results/test/TEST-*.xml'
```

### 6.2 Настроить test coverage (опционально)

Добавить Jacoco или Kover.

---

## Критические файлы для изменения

### Новые файлы:
1. `build-logic/convention/src/main/java/TestingPlugin.kt` - convention плагин
2. `core/testing/` - модуль с test utilities
3. `**/src/commonTest/` - тестовые директории для каждого модуля
4. `**/src/jvmTest/` - JVM тесты
5. `**/src/androidInstrumentedTest/` - Android UI тесты

### Изменить:
1. `gradle/libs.versions.toml` - добавить версии kotest, mockk
2. `build-logic/convention/build.gradle.kts` - зависимости плагина
3. `settings.gradle.kts` - зарегистрировать `pavlig43.testing`
4. `.github/workflows/detekt.yml` - добавить job для тестов

---

## Порядок реализации

1. **Конвенция плагин** (день 1)
2. **Test utilities** (день 1-2)
3. **Unit тесты для core** (день 2-3)
4. **Database тесты** (день 4-5)
5. **Integration тесты** (день 6-7)
6. **UI тесты** (день 8-10)
7. **CI/CD** (день 11)

---

## Проверка

```bash
# Запустить все тесты
./gradlew allTests

# Запустить тесты для конкретного модуля
./gradlew :core:test
./gradlew :database:jvmTest

# Запустить UI тесты
./gradlew :coreui:connectedAndroidTest

# Генерация отчета
./gradlew testDebugUnitTest --continue
```

---

## Исследование текущего состояния

### Текущее состояние тестирования
- **Нулевое покрытие тестами** - нет тестовых файлов
- **Не настроены test source sets** (commonTest, androidTest, jvmTest)
- **Convention плагины не добавляют тестовую конфигурацию**
- **Нет MockK для мокирования**
- **Нет тестовых утилит**

### Уже доступные зависимости (но не используются)
- `kotlin-test` (Kotlin 2.3.0)
- `kotlin-test-junit` (Kotlin 2.3.0)
- `junit` 4.13.2
- `androidx-test-junit` 1.3.0
- `turbine` 1.2.1 (для Flow)
- `koin-test` 4.1.1
- `kotlinx-coroutines-test` 1.10.2

### Что нужно добавить
- **Kotest 5.9.1** - BDD тест framework
- **Kotest Assertions** - мощные утверждения
- **MockK 1.13.12** - mocking framework
- **Room Testing** - inMemory database
- **Compose UI Testing** - UI тесты

### Архитектурные особенности для тестирования

#### Core Module
- `TabNavigationComponent` - сложная навигация с сохранением состояния
- `TransactionExecutor` - управление транзакциями БД
- `ValueToStateFlow` - трансформация состояний
- DateTime утилиты

#### Database Module
- Room с множественными сущностями: Product, Vendor, Declaration, Document, Transaction
- Сложные DAO с @Relation маппингами
- Type converters для сериализации

#### Feature Modules
- ViewModel с StateFlow реактивным состоянием
- Фильтры, сортировка, валидация
- Form логика и CRUD операции
- Decompose компоненты навигации

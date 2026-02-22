# Реализация PfComponent для OPZS транзакций

## Обзор

Реализован компонент для редактирования полуфабриката (Product Frame - Pf) в OPZS транзакциях (Отчёт производства за смену).

**Тип компонента:** Single-line (одна строка для одного полуфабриката)

## Созданные файлы

### 1. Database слой

#### `database/src/commonMain/kotlin/ru/pavlig43/database/data/transact/pf/PfBD.kt`
Entity для хранения данных о полуфабрикате в БД:

```kotlin
@Entity(tableName = "pf")
data class PfBD(
    @ColumnInfo("transaction_id") val transactionId: Int,
    @ColumnInfo("product_id") val productId: Int,
    @ColumnInfo("product_name") val productName: String,  // denormalized
    @ColumnInfo("declaration_id") val declarationId: Int,
    @ColumnInfo("declaration_name") val declarationName: String,  // denormalized
    @ColumnInfo("count") val count: Int,
    @PrimaryKey(autoGenerate = true) override val id: Int = 0
) : SingleItem
```

**Особенности:**
- Наследуется от `SingleItem` (требование для single-line компонентов)
- Содержит denormalized поля `productName` и `declarationName` для быстрого доступа без JOIN
- Внешний ключ на `Transact` с CASCADE удалением

#### `database/src/commonMain/kotlin/ru/pavlig43/database/data/transact/pf/dao/PfDao.kt`
DAO для работы с Pf:

```kotlin
@Dao
interface PfDao {
    @Query("SELECT * FROM pf WHERE transaction_id = :transactionId")
    suspend fun getByTransactionId(transactionId: Int): PfBD?

    @Upsert
    suspend fun upsert(pf: PfBD): Long
}
```

#### `database/src/commonMain/kotlin/ru/pavlig43/database/NocombroDatabase.kt`
Добавлен `pfDao` в Database:

```kotlin
abstract val pfDao: PfDao
```

### 2. DI слой

#### `features/form/transaction/.../di/CreateTransactionFormModule.kt`

**Repository:**
```kotlin
private class PfUpdateRepository(
    private val db: NocombroDatabase
) : UpdateSingleLineRepository<PfBD> {

    private val dao = db.pfDao

    override suspend fun getInit(id: Int): Result<PfBD> {
        return runCatching {
            dao.getByTransactionId(id) ?: PfBD(
                transactionId = id,
                productId = 0,
                productName = "",
                declarationId = 0,
                declarationName = "",
                count = 0,
                id = 0
            )
        }
    }

    override suspend fun update(changeSet: ChangeSet<PfBD>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)
        return runCatching {
            dao.upsert(changeSet.new)
        }
    }
}
```

**Регистрация в Koin:**
```kotlin
single<UpdateSingleLineRepository<PfBD>> { PfUpdateRepository(get()) }
```

### 3. UI слой (Single-line)

#### `PfUi.kt`
Data class для UI + мапперы:

```kotlin
data class PfUi(
    val id: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val declarationId: Int = 0,
    val declarationName: String = "",
    val count: Int = 0,
) : ISingleLineTableUi

// Мапперы
internal fun PfUi.toDto(): PfBD { /* ... */ }
internal fun PfBD.toUi(): PfUi { /* ... */ }
```

#### `PfField.kt`
Enum полей для таблицы:

```kotlin
internal enum class PfField {
    PRODUCT,
    DECLARATION,
    COUNT
}
```

#### `PfComponent.kt`
Главный компонент, наследуется от `UpdateSingleLineComponent`:

```kotlin
internal class PfComponent(
    componentContext: ComponentContext,
    transactionId: Int,
    updateSingleLineRepository: UpdateSingleLineRepository<PfBD>,
    componentFactory: SingleLineComponentFactory<PfBD, PfUi>,
    private val tabOpener: TabOpener,
    private val immutableTableDependencies: ImmutableTableDependencies,
    observeOnItem: (PfUi) -> Unit = {},
    onSuccessInitData: (PfUi) -> Unit = {},
) : UpdateSingleLineComponent<PfBD, PfUi, PfField>(
    componentContext = componentContext,
    id = transactionId,
    updateSingleLineRepository = updateSingleLineRepository,
    componentFactory = componentFactory,
    observeOnItem = observeOnItem,
    onSuccessInitData = onSuccessInitData,
    mapperToDTO = { toDto() }
) { /* ... */ }
```

**Функционал:**
- Dialog для выбора Product через MBSImmutableTable
- Dialog для выбора Declaration через MBSImmutableTable
- Валидация (продукт, декларация, count > 0)

#### `Column.kt`
Колонки таблицы:

```kotlin
internal fun createPfColumns(
    onOpenProductDialog: () -> Unit,
    onOpenDeclarationDialog: () -> Unit,
    onChangeItem: (PfUi) -> Unit,
): ImmutableList<ColumnSpec<PfUi, PfField, Unit>>
```

**Колонки:**
- Продукт (textWithSearchIconColumn)
- Декларация (textWithSearchIconColumn)
- Количество (decimalColumn с DecimalFormat.Decimal3)

#### `PfScreen.kt`
Composable экран:

```kotlin
@Composable
internal fun PfScreen(component: PfComponent) {
    val dialog by component.dialog.subscribeAsState()
    SingleLineBlockScreen(component)
    // Отображение диалогов
}
```

## Архитектурные решения

### 1. Single-line vs Multi-line
**Выбран:** Single-line (одна запись Pf на транзакцию)

**Обоснование:** Для OPZS транзакции производится один полуфабрикат.

### 2. Denormalized данные
`productName` и `declarationName` хранятся в Entity, а не получаются через @Relation.

**Причины:**
- Проще для single-line компонента
- Не нужен сложный mapper с @Relation
- Аналогично `Product` (где `displayName` в Entity)

### 3. Наследование от SingleItem
`PfBD` наследуется от `SingleItem` (не `CollectionObject`).

**Причина:** Single-line компоненты требуют `SingleItem`.

## Интеграция

Для использования PfComponent в OPZS транзакции нужно:

1. Создать `SingleLineComponentFactory<PfBD, PfUi>`:

```kotlin
val pfFactory = SingleLineComponentFactory(
    initItem = PfUi(),
    errorFactory = { pfUi ->
        buildList {
            if (pfUi.productId == 0) add("Не указан продукт")
            if (pfUi.count == 0) add("Количество равно 0")
            if (pfUi.declarationId == 0) add("Нет декларации")
        }
    },
    mapperToUi = { it.toUi() }
)
```

2. Создать компонент:

```kotlin
PfComponent(
    componentContext = context,
    transactionId = transactionId,
    updateSingleLineRepository = repository,
    componentFactory = pfFactory,
    tabOpener = tabOpener,
    immutableTableDependencies = dependencies
)
```

## Зависимости

### Зависит от:
- `UpdateSingleLineComponent` (mutable/table)
- `MBSImmutableTableComponent` (immutable)
- `SingleLineComponentFactory`
- `PfBD` (database)
- `PfDao` (database)

### Используется в:
- OPZS транзакции (TransactionFormTabsComponent)

## Тестирование

**Компиляция:**
```bash
./gradlew :database:compileKotlinDesktop
./gradlew :features:form:transaction:compileKotlinDesktop
```

**Запуск Desktop:**
```bash
./gradlew :app:desktopApp:run
```

## Следующие шаги

1. Интегрировать PfComponent в TransactionFormTabsComponent для OPZS
2. Добавить вкладку Pf в OPZS транзакцию
3. Создать UI для отображения Pf в списке транзакций

## Файлы

**Database:**
- `database/.../pf/PfBD.kt`
- `database/.../pf/dao/PfDao.kt`
- `database/.../NocombroDatabase.kt` (добавлен pfDao)

**DI:**
- `features/form/transaction/.../di/CreateTransactionFormModule.kt`

**UI:**
- `features/form/transaction/.../opzs/pf/PfComponent.kt`
- `features/form/transaction/.../opzs/pf/PfScreen.kt`
- `features/form/transaction/.../opzs/pf/PfUi.kt`
- `features/form/transaction/.../opzs/pf/PfField.kt`
- `features/form/transaction/.../opzs/pf/Column.kt`

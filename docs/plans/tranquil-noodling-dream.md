# План: Создание Feature для SafetyStock (страховой запас)

## Описание

Создать сущность SafetyStock в БД и вкладку с таблицей из одной строки в форме продукта.
Сущность показывает:
- `reorderPoint` - остаток при котором нужно заказывать
- `orderQuantity` - сколько нужно заказывать

## Архитектура

**Связь:** SafetyStock - отдельная сущность, связь с Product через productId (1:1)

## Шаги реализации

### 1. База данных

**Файл:** `database/src/commonMain/kotlin/ru/pavlig43/database/data/product/SafetyStock.kt`

```kotlin
@Entity(
    tableName = "safety_stock",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SafetyStock(
    @ColumnInfo("product_id")
    val productId: Int,

    @ColumnInfo("reorder_point")
    val reorderPoint: Int,

    @ColumnInfo("order_quantity")
    val orderQuantity: Int,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)
```

**Файл:** `database/src/commonMain/kotlin/ru/pavlig43/database/data/product/dao/SafetyStockDao.kt`

```kotlin
@Dao
interface SafetyStockDao {
    @Query("SELECT * FROM safety_stock WHERE product_id = :productId")
    suspend fun getByProductId(productId: Int): SafetyStock?

    @Upsert
    suspend fun upsert(safetyStock: SafetyStock): Long

    @Query("DELETE FROM safety_stock WHERE product_id = :productId")
    suspend fun deleteByProductId(productId: Int)
}
```

**Обновить NocombroDatabase.kt:**
- Добавить `SafetyStock::class` в entities
- Добавить `abstract val safetyStockDao: SafetyStockDao`

### 2. UI Model

**Файл:** `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/update/tabs/safety/SafetyStockUi.kt` (уже существует)

Убедиться что:
- `reorderPoint: Int` - целое число
- `orderQuantity: Int` - целое число
- Реализует `ISingleLineTableUi`

### 3. Component

**Файл:** `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/update/tabs/safety/SafetyStockComponent.kt`

```kotlin
internal class SafetyStockComponent(
    componentContext: ComponentContext,
    productId: Int,
    updateRepository: UpdateSingleLineRepository<SafetyStock>,
    componentFactory: SingleLineComponentFactory<SafetyStock, SafetyStockUi>,
    observeOnItem: (SafetyStockUi) -> Unit,
) : UpdateSingleLineComponent<SafetyStock, SafetyStockUi, SafetyStockField>(
    // ...
)
```

### 4. Columns

**Файл:** `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/update/tabs/safety/Column.kt`

Создать колонки для `reorderPoint` и `orderQuantity` с использованием `decimalColumn` или `intColumn`.

### 5. Field enum

**Файл:** `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/update/tabs/safety/SafetyStockField.kt`

```kotlin
internal enum class SafetyStockField {
    REORDER_POINT,
    ORDER_QUANTITY
}
```

### 6. Tab integration

**Файл:** `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/ProductTab.kt`

Добавить:
```kotlin
@Serializable
data object SafetyStock : ProductTab
```

**Файл:** `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/ProductTabChild.kt`

Добавить:
```kotlin
class SafetyStock(override val component: SafetyStockComponent) : ProductTabChild
```

**Файл:** `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/update/ProductFormTabsComponent.kt`

Добавить в `tabChildFactory` и `startConfigurations`.

### 7. Screen

**Файл:** `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/update/tabs/safety/SafetyStockScreen.kt`

### 8. DI

**Файл:** `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/di/ProductFormModule.kt`

Добавить зависимости для SafetyStock.

### 9. Миграция БД

Увеличить версию БД до 4 и добавить AutoMigration(3 → 4).

## Проверка

1. `./gradlew build --continue` - собрать проект
2. Проверить что вкладка SafetyStock отображается в форме продукта
3. Проверить сохранение/загрузку данных
4. Проверить CASCADE удаление при удалении продукта

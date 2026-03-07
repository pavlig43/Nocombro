# Паттерн: Создание SingleLine вкладки для сущности

## Общий шаблон

Для создания новой single-line вкладки (например, SafetyStock, Essentials) нужно:

### 1. Entity (database)

```kotlin
@Entity(tableName = "table_name", foreignKeys = [...])
data class MyEntity(
    @ColumnInfo("parent_id", index = true)
    val parentId: Int,

    @ColumnInfo("field1")
    val field1: Type,

    // ...
    override val id: Int = 0
): SingleItem
```

### 2. DAO (database)

```kotlin
@Dao
interface MyEntityDao {
    @Query("SELECT * FROM table_name WHERE parent_id = :parentId")
    suspend fun getByParentId(parentId: Int): MyEntity?

    @Upsert
    suspend fun upsert(entity: MyEntity)

    @Delete
    suspend fun delete(entity: MyEntity)
}
```

### 3. Repository (ProductFormModule.kt)

```kotlin
private class MyEntityRepository(db: NocombroDatabase) : UpdateSingleLineRepository<MyEntity> {
    private val dao = db.myEntityDao

    override suspend fun getInit(id: Int): Result<MyEntity> {
        return runCatching {
            dao.getByParentId(id) ?: MyEntity.default(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<MyEntity>): Result<Unit> {
        // Логика удаления если все значения 0
        // или просто upsert
    }
}
```

### 4. Enum Type (ProductFormModule.kt)

```kotlin
internal enum class SingleRepositoryType {
    ESSENTIALS,
    MY_ENTITY,  // добавить тип
}
```

### 5. DI Registration (ProductFormModule.kt)

```kotlin
single<UpdateSingleLineRepository<MyEntity>>(SingleRepositoryType.MY_ENTITY.qualifier) { MyEntityRepository(get()) }
```

### 6. UI Model

```kotlin
data class MyEntityUi(
    val id: Int = 0,
    val parentId: Int = 0,
    val field1: Type = defaultValue,
    // ...
)

fun MyEntity.toUi() = MyEntityUi(...)
fun MyEntityUi.toDto() = MyEntity(...)
```

### 7. Field Enum

```kotlin
internal enum class MyEntityField {
    FIELD1,
    FIELD2,
}
```

### 8. Columns

```kotlin
@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.product.internal.update.tabs.myentity

internal fun createMyEntityColumns(onChangeItem: ...) = ... {
    decimalColumn(key = MyEntityField.FIELD1, ...)
}
```

### 9. Component + Factory

```kotlin
private val myEntityComponentFactory = SingleLineComponentFactory<MyEntity, MyEntityUi>(
    initItem = MyEntityUi.default(),
    errorFactory = { validate(it) },
    mapperToUi = { toUi() }
)

internal class MyEntityComponent(
    componentContext: ComponentContext,
    parentId: Int,
    updateRepository: UpdateSingleLineRepository<MyEntity>,
) : UpdateSingleLineComponent<MyEntity, MyEntityUi, MyEntityField>(...) {
    override val title: String = "Заголовок"
}
```

### 10. Tab Integration

**ProductTab.kt:**
```kotlin
@Serializable data object MyEntity : ProductTab
```

**ProductTabChild.kt:**
```kotlin
class MyEntity(override val component: MyEntityComponent) : ProductTabChild
```

**ProductFormTabsComponent.kt:**
```kotlin
startConfigurations += ProductTab.MyEntity

ProductTab.MyEntity -> ProductTabChild.MyEntity(
    MyEntityComponent(
        updateRepository = scope.get(SingleRepositoryType.MY_ENTITY.qualifier)
    )
)
```

## Что меняется от случая к случаю

| Элемент | Пример | Уникальное |
|---------|--------|------------|
| Entity | `SafetyStock`, `Product` | Поля, tableName |
| Repository | `SafetyStockUpdateRepository` | Логика default/удаления |
| UI | `SafetyStockUi` | Поля |
| Field enum | `SafetyStockField` | Значения |
| Factory | `safetyStockComponentFactory` | Валидация, initItem |
| Component | `SafetyStockComponent` | title, columns |

## Ключевые моменты

1. **Factory в файле компонента**, не в DI
2. **SingleRepositoryType** для single-line репозиториев
3. **Index = true** на внешнем ключе в Entity
4. **toUi()/toDto()** методы для маппинга
5. **Decimal3()** для чисел с форматированием

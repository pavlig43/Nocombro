# Database Query Rules

## Используй @Relation вместо raw SQL JOINs

**ПРАВИЛО:** При составлении запросов в Room Database для получения связанных сущностей ВСЕГДА используй `@Relation` аннотацию вместо написания raw SQL JOIN запросов.

### ❌ ПЛОХО

```kotlin
@Query("""
    SELECT
        p.name as productName,
        b.count,
        d.displayName as declarationName,
        v.name as vendorName,
        p.dateBorn,
        b.price,
        b.comment,
        b.id
    FROM buy_bd b
    INNER JOIN products p ON b.product_id = p.id
    INNER JOIN declarations d ON b.declaration_id = d.id
    INNER JOIN vendors v ON d.vendor_id = v.id
""")
suspend fun getAllBuysWithDetails(): List<BuyBD>
```

### ✅ ХОРОШО

```kotlin
data class BuyBD(
    @Embedded
    val buy: BuyEntity,

    @Relation(parentColumn = "product_id", entityColumn = "id")
    val product: Product,

    @Relation(parentColumn = "declaration_id", entityColumn = "id")
    val declaration: Declaration
)

@Dao
interface BuyDao {
    @Transaction
    @Query("SELECT * FROM buy_bd")
    suspend fun getAllBuysWithDetails(): List<BuyBD>
}
```

### Преимущества `@Relation`

- **Type-safe** — Room проверяет связи на этапе компиляции
- **Автоматическая генерация** JOIN запросов
- **Легче поддерживать** и рефакторить
- **Поддержка вложенных отношений**
- **Лучше работает с KMP** (commonMain)

### Исключения (когда можно использовать @Query с JOIN)

- Сложные агрегатные запросы (`COUNT`, `SUM`, `AVG`, `GROUP BY`)
- Ручная оптимизация производительности — **только после профилирования**
- Фильтрация по связанным полям (`WHERE related.field = value`)

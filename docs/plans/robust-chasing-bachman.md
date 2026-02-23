# План реализации Storage — DAO и Repository слои

## Обзор

Новая функциональность для отображения движения продуктов по партиям за выбранный период с вложенными строками:
- **Родительская строка** — продукт с агрегацией (начальный остаток, приход, расход, конечный остаток)
- **Дочерние строки** — партии продукта с детализацией движений

---

## Структура таблицы

| Уровень | Описание | Колонки |
|---------|----------|---------|
| **Родительский** | Продукт | Продукт \| Нач. остаток \| Приход \| Расход \| Кон. остаток |
| **Дочерний** | Партия | Партия \| [пусто] \| Приход \| Расход \| [пусто] |

---

## 1. Database Layer

**Файл:** `database/src/commonMain/kotlin/ru/pavlig43/database/data/batch/dao/BatchMovementDao.kt`

### Изменить существующий `MovementOut`

Добавить @Relation для транзакции (строка 98-107):

```kotlin
internal data class MovementOut(
    @Embedded
    val movement: BatchMovement,

    @Relation(entity = BatchBD::class, parentColumn = "batch_id", entityColumn = "id")
    val batchOut: BatchOut,  // уже содержит product и declaration

    // НОВАЯ RELATION
    @Relation(entity = Transact::class, parentColumn = "transaction_id", entityColumn = "id")
    val transaction: Transact
)
```

### Добавить методы в BatchMovementDao

```kotlin
// Все движения за период (с product, batch, declaration, transaction через @Relation)
@Transaction
@Query("""
    SELECT * FROM batch_movement
    WHERE transaction_id IN (
        SELECT id FROM transact
        WHERE created_at >= :startDate AND created_at < :endDate
    )
""")
abstract fun observeMovementsInPeriod(
    startDate: LocalDateTime,
    endDate: LocalDateTime
): Flow<List<MovementOut>>

// Начальный остаток продукта до указанной даты
@Query("""
    SELECT COALESCE(SUM(
        CASE WHEN movement_type = 'INCOMING' THEN count
             WHEN movement_type = 'OUTGOING' THEN -count
        END
    ), 0)
    FROM batch_movement
    WHERE batch_id IN (SELECT id FROM batch WHERE product_id = :productId)
      AND transaction_id IN (SELECT id FROM transact WHERE created_at < :beforeDate)
""")
abstract fun observeInitialBalance(
    productId: Int,
    beforeDate: LocalDateTime
): Flow<Int>
```

---

## 2. Repository Layer

### DTO: `ProductMovements.kt`

```kotlin
data class ProductMovements(
    val productId: Int,
    val productName: String,
    val movements: List<MovementOut>
)
```

### Repository: `StorageRepository.kt`

```kotlin
class StorageRepository(
    private val batchMovementDao: BatchMovementDao
) {
    // Движения за период, сгруппированные по продукту
    fun observeMovementsGroupedByProduct(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<ProductMovements>> {
        return batchMovementDao.observeMovementsInPeriod(startDate, endDate)
            .map { movements ->
                movements.groupBy { it.batchOut.product.id }
                    .map { (productId, productMovements) ->
                        ProductMovements(
                            productId = productId,
                            productName = productMovements.first().batchOut.product.displayName,
                            movements = productMovements
                        )
                    }
                    .sortedBy { it.productId }
            }
    }

    // Начальный остаток для продукта
    fun observeInitialBalance(
        productId: Int,
        beforeDate: LocalDateTime
    ): Flow<Int> {
        return batchMovementDao.observeInitialBalance(productId, beforeDate)
    }
}
```

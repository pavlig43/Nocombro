# План: Исправление дублирования движений в PF и Ингредиентах OPZS

## Проблема

При повторном сохранении PF и ингредиентов OPZS без перезагрузки вкладки создаются дубликаты batch_movement. Компонент держит `movementId=0` после первого сохранения, т.к. `update()` возвращает `Result<Unit>` и ID не попадают обратно в UI.

| Тип | Создаёт Batch? | Движение | Возвращает ID? | Баг? |
|-----|---------------|----------|----------------|------|
| BUY | Да | INCOMING | Да | Нет |
| SALE | Нет | OUTGOING | Да | Нет |
| **PF** | Да | INCOMING | **Нет** | **Да** |
| **Ингредиенты** | Нет | OUTGOING | **Нет** | **Да** |

## Решение: Defensive check в репозиториях

Перед созданием движения проверять, существует ли уже движение для этой транзакции. Минимальное изменение — только репозитории + один DAO-запрос. Без изменения интерфейсов.

## Изменения

### 1. Добавить query в BatchMovementDao

**Файл:** `database/src/desktopMain/kotlin/ru/pavlig43/database/data/batch/dao/BatchMovementDao.kt`

Добавить метод для поиска существующего OUTGOING движения по batchId + transactionId (для ингредиентов):

```kotlin
@Query("""
    SELECT * FROM batch_movement
    WHERE transaction_id = :transactionId
    AND batch_id = :batchId
    AND movement_type = 'OUTGOING'
    LIMIT 1
""")
abstract suspend fun findOutgoingByBatchAndTransaction(
    transactionId: Int,
    batchId: Int
): BatchMovement?
```

### 2. Исправить PfUpdateRepository.update()

**Файл:** `features/form/transaction/src/desktopMain/kotlin/ru/pavlig43/transaction/internal/di/CreateTransactionFormModule.kt:239-273`

Перед созданием batch/movement проверить, существует ли уже INCOMING движение через `movementDao.getByTransactionId()`:

```kotlin
val existingMovement = if (pf.movementId == 0) {
    movementDao.getByTransactionId(pf.transactionId)
        .firstOrNull { it.movement.movementType == MovementType.INCOMING }
} else null

val effectiveBatchId = existingMovement?.movement?.batchId ?: pf.batchId
val effectiveMovementId = existingMovement?.movement?.id ?: pf.movementId
```

Затем использовать `effectiveBatchId` и `effectiveMovementId` вместо `pf.batchId` и `pf.movementId`.

### 3. Исправить IngredientsCollectionRepository.upsertIngredients()

**Файл:** тот же `CreateTransactionFormModule.kt:298-311`

Заменить bulk `upsertMovements()` на итерацию с defensive check:

```kotlin
private suspend fun upsertIngredients(ingredients: List<IngredientBD>) {
    ingredients.forEach { ingredient ->
        val effectiveMovementId = if (ingredient.movementId == 0 && ingredient.batchId != 0) {
            movementDao.findOutgoingByBatchAndTransaction(
                transactionId = ingredient.transactionId,
                batchId = ingredient.batchId
            )?.id ?: 0
        } else {
            ingredient.movementId
        }

        val movement = BatchMovement(
            batchId = ingredient.batchId,
            movementType = MovementType.OUTGOING,
            count = ingredient.count,
            transactionId = ingredient.transactionId,
            id = effectiveMovementId
        )
        if (movement.id == 0) {
            movementDao.createMovement(movement)
        } else {
            movementDao.upsertMovement(movement)
        }
    }
}
```

## Итого

- **2 файла** изменяются
- ~30 строк нового/изменённого кода
- Без изменения интерфейсов `UpdateSingleLineRepository` / `UpdateCollectionRepository`
- Без изменения компонентного слоя

## Верификация

1. Создать OPZS отчёт, заполнить PF и ингредиенты, сохранить
2. Не закрывая вкладку, изменить количество PF и/или ингредиентов
3. Сохранить снова
4. Проверить в БД: должно быть одно INCOMING движение для PF и по одному OUTGOING на каждый ингредиент (без дубликатов)

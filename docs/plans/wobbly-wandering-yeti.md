# План: Изменить StorageDao на возвращение List<StorageProduct>

## Изменения в `StorageDao.kt`

Изменить функцию `observeOnStorageBatches`:
1. Тип `storageBatches`: `Flow<List<StorageProduct>>`
2. Группировка: сначала по продукту, затем по батчам внутри продукта
3. Использовать `movementOuts.first().batchOut.product` для productId/productName
4. Использовать `sign` для расчётов
5. Оставить `.mapValues { "$it \n" }` в конце

```kotlin
fun observeOnStorageBatches(
    start: LocalDateTime,
    end: LocalDateTime
): Flow<List<String>> {
    val storageBatches: Flow<List<StorageProduct>> = observeOnAllMovements().map { fillList ->
        val filteredList = fillList.filter { it.transaction.createdAt <= end }
        filteredList.groupBy { it.batchOut.product }

            .run {
                this.values.mapParallel(Dispatchers.IO) { movementOuts: List<MovementOut> ->
                    val product = movementOuts.first().batchOut.product
                    val productId = product.id
                    val productName = product.displayName

                    val batches = movementOuts
                        .groupBy { it.batchOut.batch }
                        .map { (batch, moves) ->
                            val batchId = batch.id
                            val batchName = "($batchId) ${batch.dateBorn.format(dateFormat)}"
                            var balanceBeforeStart = 0
                            var incoming = 0
                            var outgoing = 0

                            moves.forEach { move ->
                                val count = move.movement.count
                                val type = move.movement.movementType
                                val transactionDt = move.transaction.createdAt
                                val sign = when(type) {
                                    MovementType.INCOMING -> 1
                                    MovementType.OUTGOING -> -1
                                }

                                when {
                                    transactionDt < start -> balanceBeforeStart += count * sign
                                    else -> {
                                        if (type == MovementType.INCOMING) incoming += count
                                        else outgoing += count
                                    }
                                }
                            }

                            StorageBatch(
                                batchId = batchId,
                                batchName = batchName,
                                balanceBeforeStart = balanceBeforeStart,
                                incoming = incoming,
                                outgoing = outgoing,
                                balanceOnEnd = balanceBeforeStart + incoming - outgoing
                            )
                        }

                    StorageProduct(
                        productId = productId,
                        productName = productName,
                        batches = batches
                    )
                }
            }
    }
    return storageBatches.mapValues { "$it \n" }
}
```

## Файл для изменения
- `database/src/commonMain/kotlin/ru/pavlig43/database/data/storage/dao/StorageDao.kt`

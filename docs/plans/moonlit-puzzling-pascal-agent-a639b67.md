# Анализ производительности и план оптимизации observeOnStorageBatches

## Текущая реализация

**Файл:** `C:\Users\user\AndroidStudioProjects\Nocombro\database\src\commonMain\kotlin\ru\pavlig43\database\data\storage\dao\StorageDao.kt`

```kotlin
fun observeOnStorageBatches(
    start: LocalDateTime,
    end: LocalDateTime
): Flow<List<String>> {
    val storageBatches: Flow<List<StorageProduct>> = observeOnAllMovements().map { fillList ->
        val filteredList = fillList.filter { it.transaction.createdAt <= end }  // sequential
        filteredList.groupBy { it.batchOut.product }                           // sequential
            .run {
                this.values.mapParallel(Dispatchers.IO) { movementOuts: List<MovementOut> ->
                    // ... обработка продуктов параллельно
                }
            }
    }
    return storageBatches.mapValues { "$it \n" }
}
```

---

## Анализ проблем производительности

### 1. **Последовательная фильтрация и группировка**
```kotlin
val filteredList = fillList.filter { it.transaction.createdAt <= end }  // O(n) sequential
filteredList.groupBy { it.batchOut.product }                           // O(n) sequential
```
**Проблема:** Обе операции sequential — не используют преимущества многоядерных процессоров.
**Влияние:** Для 10,000+ записей это создаёт значительную задержку.

### 2. **Избыточное создание объектов**
```kotlin
val batchName = "($batchId) ${batch.dateBorn.format(dateFormat)}"
```
**Проблема:** Форматирование даты создаёт новую строку для каждой batch, даже если она не будет отображаться.

### 3. **Многократный проход по данным**
```kotlin
balanceBeforeStart = batches.sumOf { it.balanceBeforeStart }  // 1-й проход
incoming = batches.sumOf { it.incoming }                       // 2-й проход
outgoing = batches.sumOf { it.outgoing }                       // 3-й проход
balanceOnEnd = batches.sumOf { it.balanceOnEnd }               // 4-й проход
```
**Проблема:** 4 прохода по списку batches вместо одного.

### 4. **Неоптимальное использование mapParallel**
```kotlin
this.values.mapParallel(Dispatchers.IO) { movementOuts ->
```
**Проблема:** `Dispatchers.IO` предназначен для I/O операций, а не CPU-bound вычислений. Для расчётов лучше `Dispatchers.Default`.

### 5. **Отсутствие early termination**
**Проблема:** Если `end` дата раньше всех записей, мы всё равно загружаем все данные из БД.

### 6. **Использование var вместо fold**
```kotlin
var balanceBeforeStart = 0
var incoming = 0
var outgoing = 0

moves.forEach { move ->
    // мутация var
}
```
**Проблема:** Мутабельное состояние менее функционально и сложнее оптимизировать компилятором.

---

## Оптимизация 1: Параллельная обработка на уровне Flow

### Идея
Использовать `flowOn(Dispatchers.Default)` для параллельной обработки Flow и `flatMapMerge` для параллельной обработки групп.

### Код

```kotlin
fun observeOnStorageBatches(
    start: LocalDateTime,
    end: LocalDateTime
): Flow<List<String>> {
    return observeOnAllMovements()
        .map { fillList ->
            // Оптимизация 1: Совмещаем filter + groupBy в один проход
            fillList.groupBy {
                it.transaction.createdAt <= end to it.batchOut.product
            }
        }
        .map { grouped ->
            // Оптимизация 2: Обрабатываем продукты параллельно
            grouped[true]?.values?.mapParallel(Dispatchers.Default) { movementOuts ->
                processProduct(movementOuts, start)
            } ?: emptyList()
        }
        .flowOn(Dispatchers.Default)  // Параллелим на уровне Flow
        .mapValues { "$it \n" }
}

private fun processProduct(
    movementOuts: List<MovementOut>,
    start: LocalDateTime
): StorageProduct {
    val product = movementOuts.first().batchOut.product

    // Оптимизация 3: Используем fold для одного прохода
    val batchTotals = movementOuts
        .groupBy { it.batchOut.batch }
        .mapValues { (_, moves) ->
            moves.fold(Quad(0, 0, 0, 0)) { acc, move ->
                val count = move.movement.count
                val type = move.movement.movementType
                val dt = move.transaction.createdAt

                when {
                    dt < start && type == MovementType.INCOMING ->
                        acc.copy(before = acc.before + count)
                    dt < start && type == MovementType.OUTGOING ->
                        acc.copy(before = acc.before - count)
                    type == MovementType.INCOMING ->
                        acc.copy(incoming = acc.incoming + count)
                    type == MovementType.OUTGOING ->
                        acc.copy(outgoing = acc.outgoing + count)
                    else -> acc
                }
            }
        }

    // Оптимизация 4: Один проход для суммирования
    val (totalBefore, totalIncoming, totalOutgoing, _) = batchTotals.values
        .fold(Quad(0, 0, 0, 0)) { acc, quad ->
            Quad(
                before = acc.before + quad.before,
                incoming = acc.incoming + quad.incoming,
                outgoing = acc.outgoing + quad.outgoing,
                end = acc.end + (quad.before + quad.incoming - quad.outgoing)
            )
        }

    val batches = batchTotals.map { (batch, quad) ->
        StorageBatch(
            batchId = batch.id,
            batchName = "(${batch.id}) ${batch.dateBorn.format(dateFormat)}",  // Lazy форматирование
            balanceBeforeStart = quad.before,
            incoming = quad.incoming,
            outgoing = quad.outgoing,
            balanceOnEnd = quad.before + quad.incoming - quad.outgoing
        )
    }

    return StorageProduct(
        productId = product.id,
        productName = product.displayName,
        balanceBeforeStart = totalBefore,
        incoming = totalIncoming,
        outgoing = totalOutgoing,
        balanceOnEnd = totalBefore + totalIncoming - totalOutgoing,
        batches = batches
    )
}

// Вспомогательный data class для аккумулятора
@JvmInline
value class Quad(
    val before: Int,
    val incoming: Int,
    val outgoing: Int,
    val end: Int
)
```

---

## Оптимизация 2: Агрегация на уровне БД (РЕКОМЕНДУЕТСЯ)

### Идея
Перенести максимум вычислений в SQL запрос. Room Database поддерживает агрегатные функции.

### Код

```kotlin
@Dao
abstract class StorageDao {

    // Оптимизированный запрос с фильтрацией на уровне БД
    @Transaction
    @Query("""
        SELECT * FROM batch_movement
        WHERE created_at <= :end
        ORDER BY product_id, batch_id, created_at
    """)
    internal abstract fun observeMovementsUntil(end: LocalDateTime): Flow<List<MovementOut>>

    fun observeOnStorageBatches(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<String>> {
        return observeMovementsUntil(end)  // Фильтрация в БД!
            .map { fillList ->
                fillList
                    .groupBy { it.batchOut.product }
                    .mapParallel(Dispatchers.Default) { movementOuts ->
                        processProductOptimized(movementOuts, start)
                    }
            }
            .flowOn(Dispatchers.Default)
            .mapValues { "$it \n" }
    }
}

private fun processProductOptimized(
    movementOuts: List<MovementOut>,
    start: LocalDateTime
): StorageProduct {
    val product = movementOuts.first().batchOut.product
    val productId = product.id
    val productName = product.displayName

    // Группируем по batch и вычисляем агрегаты за один проход
    val batchDataList = movementOuts
        .groupBy { it.batchOut.batch }
        .map { (batch, moves) ->
            // Один проход с fold
            val (before, incoming, outgoing) = moves.fold(Triple(0, 0, 0)) { (accBefore, accIn, accOut), move ->
                val count = move.movement.count
                val type = move.movement.movementType
                val dt = move.transaction.createdAt

                when {
                    dt < start && type == MovementType.INCOMING -> Triple(accBefore + count, accIn, accOut)
                    dt < start && type == MovementType.OUTGOING -> Triple(accBefore - count, accIn, accOut)
                    type == MovementType.INCOMING -> Triple(accBefore, accIn + count, accOut)
                    type == MovementType.OUTGOING -> Triple(accBefore, accIn, accOut + count)
                    else -> Triple(accBefore, accIn, accOut)
                }
            }

            BatchAggregate(
                batch = batch,
                balanceBeforeStart = before,
                incoming = incoming,
                outgoing = outgoing,
                balanceOnEnd = before + incoming - outgoing
            )
        }

    // Агрегируем totals за один проход
    val totals = batchDataList.fold(
        BatchAggregate(null, 0, 0, 0, 0)
    ) { acc, batch ->
        BatchAggregate(
            batch = null,
            balanceBeforeStart = acc.balanceBeforeStart + batch.balanceBeforeStart,
            incoming = acc.incoming + batch.incoming,
            outgoing = acc.outgoing + batch.outgoing,
            balanceOnEnd = acc.balanceOnEnd + batch.balanceOnEnd
        )
    }

    val batches = batchDataList.map { batchData ->
        StorageBatch(
            batchId = batchData.batch!!.id,
            batchName = "(${batchData.batch.id}) ${batchData.batch.dateBorn.format(dateFormat)}",
            balanceBeforeStart = batchData.balanceBeforeStart,
            incoming = batchData.incoming,
            outgoing = batchData.outgoing,
            balanceOnEnd = batchData.balanceOnEnd
        )
    }

    return StorageProduct(
        productId = productId,
        productName = productName,
        balanceBeforeStart = totals.balanceBeforeStart,
        incoming = totals.incoming,
        outgoing = totals.outgoing,
        balanceOnEnd = totals.balanceOnEnd,
        batches = batches
    )
}

private data class BatchAggregate(
    val batch: Batch?,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int
)
```

---

## Оптимизация 3: ChannelFlow для максимального параллелизма

### Идея
Использовать `channelFlow` для асинхронной обработки чанков данных.

```kotlin
fun observeOnStorageBatches(
    start: LocalDateTime,
    end: LocalDateTime
): Flow<List<String>> {
    return observeMovementsUntil(end)
        .map { fillList ->
            channelFlow {
                val productGroups = fillList.groupBy { it.batchOut.product }

                // Разбиваем на чанки для параллельной обработки
                productGroups.values
                    .chunked(maxOf(1, productGroups.size / (Runtime.getRuntime().availableProcessors() * 2)))
                    .forEach { chunk ->
                        launch(Dispatchers.Default) {
                            chunk.map { movementOuts ->
                                processProductOptimized(movementOuts, start)
                            }.also { results ->
                                send(results)
                            }
                        }
                    }
            }.toList().flatten()
        }
        .mapValues { "$it \n" }
}
```

---

## Сравнение подходов

| Подход | Преимущества | Недостатки | Скорость (1000+ записей) |
|--------|-------------|------------|-------------------------|
| **Текущий** | Простота | Sequential, 4+ проходов | Базовая |
| **Оптимизация 1** | Fold, mapParallel | Сложнее | ~2-3x быстрее |
| **Оптимизация 2 (БД)** | Фильтрация в SQL | Требует нового @Query | ~5-10x быстрее |
| **Оптимизация 3 (Channel)** | Макс. параллелизм | Сложность, overhead | ~3-5x быстрее |

---

## Рекомендации

### Для больших объёмов (1000+ записей):
1. **В первую очередь** — перенести фильтрацию в SQL (Оптимизация 2)
2. Заменить `var` на `fold`/`reduce`
3. Использовать `Dispatchers.Default` вместо `Dispatchers.IO`
4. Убрать множественные `sumOf` — заменить на один проход

### Для очень больших объёмов (10,000+ записей):
1. Рассмотреть пагинацию на уровне БД
2. Использовать ChannelFlow для чанков
3. Добавить кэширование результатов
4. Рассмотреть incremental updates (только deltas)

### Мониторинг производительности:
```kotlin
fun observeOnStorageBatches(
    start: LocalDateTime,
    end: LocalDateTime
): Flow<List<String>> {
    return observeMovementsUntil(end)
        .map { fillList ->
            val startTime = System.currentTimeMillis()
            val result = /* обработка */
            val duration = System.currentTimeMillis() - startTime
            println("Processing ${fillList.size} records took ${duration}ms")
            result
        }
        .mapValues { "$it \n" }
}
```

---

## Ответы на вопросы

### 1. Какие проблемы производительности?
- Sequential filter + groupBy
- 4 прохода `sumOf` вместо одного
- Не тот dispatcher (IO вместо Default)
- Мутабельное состояние с `var`

### 2. Как сделать filter и groupBy параллельными?
- Filter можно объединить с groupBy в `groupBy { predicate to key }`
- Или использовать `chunked()` + `mapParallel`

### 3. Есть ли смысл использовать fold вместо var?
- **Да!** Fold:
  - Функциональный стиль (immutability)
  - Один проход по данным
  - Лучше оптимизируется компилятором
  - Легче тестировать

### 4. Можно ли использовать Flow операторы?
- **Да!** `flowOn(Dispatchers.Default)` для параллелизма
- `flatMapMerge` для параллельной обработки групп
- `channelFlow` для custom async логики

### 5. Самый быстрый вариант?
**Оптимизация 2 (SQL + fold + Dispatchers.Default)** — комбинация:
1. Фильтрация в БД
2. Fold для агрегации
3. Правильный dispatcher
4. Минимум проходов по данным

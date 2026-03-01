# План реализации Storage — Логика получения данных

## Обзор

Функциональность для получения данных о движении продуктов по партиям за выбранный период.

**Текущий фокус: Database и Repository слои**

---

## Что нужно реализовать

### Родительская строка (Продукт)
| Поле | Описание |
|------|----------|
| productName | Название продукта |
| openingBalance | Начальный остаток (до startDate) |
| incoming | Приход за период |
| outgoing | Расход за период |
| closingBalance | Конечный остаток (= opening + incoming - outgoing) |

### Дочерняя строка (Партия)
| Поле | Описание |
|------|----------|
| vendorName | Поставщик |
| declarationName | Декларация |
| dateBorn | Дата производства |
| incoming | Приход за период |
| outgoing | Расход за период |

---

## Критические файлы для изменения

| Файл | Изменения |
|------|-----------|
| `database/src/commonMain/kotlin/ru/pavlig43/database/data/batch/dao/BatchMovementDao.kt` | Добавить @Relation для Transact, новые DTO и методы |
| `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/data/StorageRepository.kt` | **Новый** - Repository слой |
| `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/data/dto/StorageDto.kt` | **Новый** - DTO классы |

---

## 1. Database Layer

### Файл: `database/src/commonMain/kotlin/ru/pavlig43/database/data/batch/dao/BatchMovementDao.kt`

#### Изменение 1.1: Добавить @Relation к MovementOut (строка 99-108)

**Текущий код:**
```kotlin
internal data class MovementOut(
    @Embedded
    val movement: BatchMovement,
    @Relation(entity = BatchBD::class, parentColumn = "batch_id", entityColumn = "id")
    val batchOut: BatchOut
)
```

**Нужно добавить:**
```kotlin
@Relation(entity = Transact::class, parentColumn = "transaction_id", entityColumn = "id")
val transact: Transact  // НОВАЯ RELATION
```

#### Изменение 1.2: Добавить новые DTO классы (в конец файла)

```kotlin
/**
 * Агрегированные данные по продукту для таблицы Склад.
 */
data class ProductStorageOut(
    val productId: Int,
    val productName: String,
    val openingBalance: Int,  // Начальный остаток (до startDate)
    val incoming: Int,        // Приход за период
    val outgoing: Int,        // Расход за период
    val closingBalance: Int   // Конечный остаток
)

/**
 * Движения партии для дочерних строк.
 */
data class BatchMovementOut(
    val batchId: Int,
    val vendorName: String,
    val dateBorn: kotlinx.datetime.LocalDate,
    val declarationName: String,
    val incoming: Int,
    val outgoing: Int
)
```

#### Изменение 1.3: Добавить новые методы в BatchMovementDao

**Метод 1: Получить движения партий продукта за период**
```kotlin
/**
 * Получает движения партий для продукта за период.
 * Использует существующий observeMovementsByProductId с фильтрацией по периоду.
 */
fun observeBatchMovementsByProductInPeriod(
    productId: Int,
    startDate: kotlinx.datetime.LocalDateTime? = null,
    endDate: kotlinx.datetime.LocalDateTime? = null
): Flow<List<BatchMovementOut>>
```

**Реализация:** Фильтруем `MovementOut` по `transact.createdAt`, группируем по `batchId`, суммируем incoming/outgoing.

**Метод 2: Получить начальный остаток продукта**
```kotlin
/**
 * Получает начальный остаток продукта до указанной даты.
 */
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
    beforeDate: kotlinx.datetime.LocalDateTime
): Flow<Int>
```

**Метод 3: Получить все продукты с движениями**
```kotlin
/**
 * Получает список всех продуктов, у которых есть партии.
 */
@Query("""
    SELECT DISTINCT p.id, p.displayName
    FROM product p
    INNER JOIN batch b ON p.id = b.product_id
    ORDER BY p.displayName
""")
abstract fun observeProductsWithBatches(): Flow<List<ProductIdName>>
```

---

## 2. Repository Layer

### Файл: `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/data/StorageRepository.kt`

```kotlin
package ru.pavlig43.storage.data

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.batch.dao.ProductStorageOut
import ru.pavlig43.database.data.batch.dao.BatchMovementOut

interface StorageRepository {
    /**
     * Получает агрегированные данные по складу за период.
     */
    fun observeStorageByPeriod(
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): Flow<List<ProductStorageOut>>

    /**
     * Получает движения партий для продукта.
     */
    fun observeBatchMovements(
        productId: Int,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): Flow<List<BatchMovementOut>>
}

class StorageRepositoryImpl(
    private val dao: BatchMovementDao
) : StorageRepository {

    override fun observeStorageByPeriod(
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): Flow<List<ProductStorageOut>> {
        // TODO: Implement aggregation by product
        // Нужно:
        // 1. Получить список продуктов (observeProductsWithBatches)
        // 2. Для каждого продукта получить начальный остаток (observeInitialBalance)
        // 3. Для каждого продукта получить incoming/outgoing за период
        // 4. Рассчитать closingBalance = opening + incoming - outgoing
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }

    override fun observeBatchMovements(
        productId: Int,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): Flow<List<BatchMovementOut>> {
        return dao.observeBatchMovementsByProductInPeriod(productId, startDate, endDate)
    }
}
```

### Файл: `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/data/dto/StorageDto.kt`

```kotlin
package ru.pavlig43.storage.data.dto

import kotlinx.datetime.LocalDateTime

/**
 * Период фильтрации для таблицы Склад.
 */
data class StoragePeriod(
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null
)
```

---

## Порядок реализации

### Phase 1: Database Layer
1. ✏️ Изменить `MovementOut` — добавить @Relation для Transact
2. ✏️ Добавить DTO: `ProductStorageOut`, `BatchMovementOut`
3. ✏️ Добавить методы в `BatchMovementDao`:
   - `observeBatchMovementsByProductInPeriod` (с реализацией)
   - `observeInitialBalance` (SQL запрос)
   - `observeProductsWithBatches` (SQL запрос)

### Phase 2: Repository Layer
1. ✏️ Создать `StorageRepository.kt` с интерфейсом
2. ✏️ Создать `StorageRepositoryImpl` с реализацией `observeStorageByPeriod`
3. ✏️ Создать `StorageDto.kt`

---

## Решения по требованиям

- **Начальный остаток**: Если `startDate = null`, считать как сумму всех движений (с первого движения)
- **Список продуктов**: Показывать все продукты, у которых есть партии (даже без движений за период)

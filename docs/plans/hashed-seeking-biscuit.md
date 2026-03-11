# План: Расчёт себестоимости и отчёт по продажам

## Цель

Создать модуль для расчёта себестоимости OPZS (производства) и генерации отчёта по продажам с маржой.

## Требования

1. **Себестоимость считается на лету** (не кэшировать в БД)
2. **Рекурсия ПФ** — полуфабрикат может содержать другие ПФ
3. **Общие расходы** распределяются пропорционально стоимости материалов за период
4. **Отчёт по продажам** с колонками: Дата | Транзакция | Продукт | Кол-во | Цена продажи | Себестоимость | Маржа | Прибыль
5. **Выбор периода** пользователем

---

## Архитектура

```
UI: SalesReportScreen
  ↓
Component: SalesReportComponent
  ↓
UseCase: GenerateSalesReportUseCase, CalculateOpzsCostUseCase
  ↓
Repository: CostCalculationRepository
  ↓
DAO: TransactionDao, BuyDao, ExpenseDao, IngredientDao, BatchMovementDao
```

---

## Этап 1: Расширения DAO

### Файл: `database/src/commonMain/kotlin/ru/pavlig43/database/data/transact/dao/TransactionDao.kt`

Добавить запрос:
```kotlin
@Query("""
    SELECT * FROM transact
    WHERE transaction_type = :type
    AND created_at BETWEEN :start AND :end
    ORDER BY created_at DESC
""")
suspend fun getTransactionsByTypeAndPeriod(
    type: TransactionType,
    start: LocalDateTime,
    end: LocalDateTime
): List<Transact>
```

### Файл: `database/src/commonMain/kotlin/ru/pavlig43/database/data/transact/buy/dao/BuyDao.kt`

Добавить запрос:
```kotlin
@Query("""
    SELECT b.price FROM buy_bd b
    INNER JOIN batch_movement bm ON b.movement_id = bm.id
    WHERE bm.batch_id = :batchId
    LIMIT 1
""")
suspend fun getPriceByBatchId(batchId: Int): Int
```

### Файл: `database/src/commonMain/kotlin/ru/pavlig43/database/data/batch/dao/BatchMovementDao.kt`

Добавить запросы:
```kotlin
@Query("""
    SELECT * FROM batch_movement
    WHERE transaction_id = :transactionId
    AND movement_type = 'INCOMING'
    LIMIT 1
""")
suspend fun getIncomingMovement(transactionId: Int): BatchMovement?

@Query("""
    SELECT t.id FROM transact t
    INNER JOIN batch_movement bm ON t.id = bm.transaction_id
    WHERE bm.batch_id = :batchId
    AND t.transaction_type = 'OPZS'
    LIMIT 1
""")
suspend fun getOpzsTransactionForBatch(batchId: Int): Int
```

---

## Этап 2: Создание feature модуля `costcalculation`

### Структура:
```
features/costcalculation/
├── src/commonMain/kotlin/ru/pavlig43/costcalculation/
│   ├── internal/data/
│   │   ├── model/
│   │   │   ├── CostResult.kt
│   │   │   ├── SalesReportItem.kt
│   │   │   └── SalesReportSummary.kt
│   │   └── CostCalculationRepositoryImpl.kt
│   ├── internal/domain/
│   │   ├── usecase/
│   │   │   ├── CalculateOpzsCostUseCase.kt
│   │   │   └── GenerateSalesReportUseCase.kt
│   │   └── CostCalculationRepository.kt
│   ├── internal/ui/
│   │   ├── SalesReportComponent.kt
│   │   ├── SalesReportScreen.kt
│   │   └── mapper/
│   │       └── CostToUiMapper.kt
│   ├── internal/di/
│   │   └── CostCalculationModule.kt
│   └── api/
│       └── CostCalculationDependencies.kt
```

### DTO модели:

**CostResult.kt:**
```kotlin
data class CostResult(
    val transactionId: Int,
    val materialsCost: Int,        // копейки
    val directExpenses: Int,       // копейки
    val generalExpenses: Int,      // копейки
    val totalCost: Int,            // копейки
    val costPerUnit: Int,          // копеек за грамм
    val calculatedAt: LocalDateTime
)
```

**SalesReportItem.kt:**
```kotlin
data class SalesReportItem(
    val transactionId: Int,
    val date: LocalDate,
    val productName: String,
    val quantity: Int,              // граммы
    val salePrice: Int,             // копейки
    val costPrice: Int,             // копейки (расчёт)
    val margin: Int,                // копейки
    val marginPercent: Double,
    val clientName: String? = null
)
```

**SalesReportSummary.kt:**
```kotlin
data class SalesReportSummary(
    val totalRevenue: Int,
    val totalCost: Int,
    val totalProfit: Int,
    val averageMarginPercent: Double,
    val itemsCount: Int
)
```

---

## Этап 3: Repository реализация

**CostCalculationRepository.kt** (интерфейс):
```kotlin
interface CostCalculationRepository {
    suspend fun calculateOpzsCost(
        transactionId: Int,
        periodStart: LocalDateTime,
        periodEnd: LocalDateTime
    ): Result<CostResult>

    suspend fun generateSalesReport(
        periodStart: LocalDateTime,
        periodEnd: LocalDateTime
    ): Result<Pair<List<SalesReportItem>, SalesReportSummary>>

    suspend fun getBuyPriceForBatch(batchId: Int): Result<Int>
    suspend fun getOpzsTransactionForBatch(batchId: Int): Result<Int>
}
```

**Ключевой алгоритм (псевдокод):**
```kotlin
suspend fun calculateOpzsCost(
    transactionId: Int,
    periodStart: LocalDateTime,
    periodEnd: LocalDateTime,
    cache: MutableMap<Int, CostResult> = mutableMapOf()
): CostResult {
    // 1. Проверить кэш
    cache[transactionId]?.let { return it }

    // 2. Получить ингредиенты
    val ingredients = ingredientDao.getByTransactionId(transactionId)

    // 3. Рассчитать стоимость материалов (рекурсивно)
    var materialsCost = 0
    for (ingredient in ingredients) {
        val cost = when (ingredient.productType) {
            FOOD_BASE, PACK -> getBuyPriceForBatch(ingredient.batchId)
            FOOD_PF -> {
                val opzsId = getOpzsTransactionForBatch(ingredient.batchId)
                calculateOpzsCost(opzsId, periodStart, periodEnd, cache).costPerUnit
            }
        }
        materialsCost += cost * ingredient.count
    }

    // 4. Прямые расходы
    val directExpenses = expenseDao.getByTransactionId(transactionId)
        .sumOf { it.amount }

    // 5. Доля общих расходов
    val generalExpenses = allocateGeneralExpenses(materialsCost, periodStart, periodEnd)

    // 6. Итого
    val result = CostResult(...)
    cache[transactionId] = result
    return result
}

private suspend fun allocateGeneralExpenses(
    materialsCost: Int,
    periodStart: LocalDateTime,
    periodEnd: LocalDateTime
): Int {
    val allGeneral = expenseDao.getAll()
        .filter { it.transactionId == null }
        .filter { it.expenseDateTime in periodStart..periodEnd }
        .sumOf { it.amount }

    val totalMaterials = getTotalMaterialsCostForPeriod(periodStart, periodEnd)

    if (totalMaterials == 0) return 0
    return (allGeneral * materialsCost) / totalMaterials
}
```

---

## Этап 4: UseCase слой

**GenerateSalesReportUseCase.kt:**
```kotlin
class GenerateSalesReportUseCase(
    private val repository: CostCalculationRepository
) {
    suspend operator fun invoke(
        periodStart: LocalDateTime,
        periodEnd: LocalDateTime
    ): Result<Pair<List<SalesReportItem>, SalesReportSummary>> {
        return repository.generateSalesReport(periodStart, periodEnd)
    }
}
```

---

## Этап 5: UI слой

**SalesReportComponent.kt:**
```kotlin
internal class SalesReportComponent(
    componentContext: ComponentContext,
    private val generateSalesReportUseCase: GenerateSalesReportUseCase
) : ComponentContext by componentContext {
    private val _state = MutableStateFlow<SalesReportState>(SalesReportState.Initial)
    val state: StateFlow<SalesReportState> = _state.asStateFlow()

    fun onPeriodChanged(startDate: LocalDate, endDate: LocalDate) {
        scope.launch {
            _state.value = SalesReportState.Loading
            val result = generateSalesReportUseCase(
                startDate.atTime(0, 0),
                endDate.atTime(23, 59)
            )
            _state.value = result.fold(
                onSuccess = { (items, summary) -> SalesReportState.Success(items, summary) },
                onFailure = { SalesReportState.Error(it.message) }
            )
        }
    }
}

sealed interface SalesReportState {
    data object Initial : SalesReportState
    data object Loading : SalesReportState
    data class Success(val items: List<SalesReportItemUi>, val summary: SalesReportSummaryUi) : SalesReportState
    data class Error(val message: String?) : SalesReportState
}
```

**SalesReportScreen.kt:**
- Дата-пикер для выбора периода
- Таблица с колонками: Дата | Продукт | Кол-во | Цена | Себестоимость | Маржа | Маржа %
- Футер с итогами: Доход | Себестоимость | Прибыль | Средняя маржа

---

## Этап 6: DI и интеграция

**CostCalculationModule.kt:**
```kotlin
internal fun createCostCalculationModule(db: NocombroDatabase) = module {
    single<CostCalculationRepository> { CostCalculationRepositoryImpl(db) }
    singleOf(::GenerateSalesReportUseCase)
    singleOf(::CalculateOpzsCostUseCase)
}
```

Интеграция в `app/desktopApp/...` и `app/nocombroapp/...` через CostCalculationDependencies.

---

## Критические файлы для модификации

| Файл | Что добавить |
|------|--------------|
| `database/.../TransactionDao.kt` | `getTransactionsByTypeAndPeriod()` |
| `database/.../BuyDao.kt` | `getPriceByBatchId()` |
| `database/.../BatchMovementDao.kt` | `getIncomingMovement()`, `getOpzsTransactionForBatch()` |
| `core/...` (новый) | `CostCalculationDependencies` |
| `app/desktopApp/...` | Регистрация `CostCalculationModule` |
| `app/nocombroapp/...` | Регистрация `CostCalculationModule` |

---

## Порядок реализации

1. **Расширить DAO** — добавить новые запросы
2. **Создать feature модуль** `costcalculation`
3. **Repository** — реализовать алгоритм расчёта
4. **UseCase** — создать use cases
5. **UI** — компонент и экран
6. **DI** — модуль и зависимости
7. **Навигация** — добавить пункт меню

---

## Верификация

1. **Тест алгоритма:**
   - Создать OPZS с базовыми ингредиентами
   - Создать OPZS с ПФ-ингредиентом
   - Добавить прямые и общие расходы
   - Проверить расчёт себестоимости

2. **Тест отчёта:**
   - Создать несколько SALE за период
   - Сгенерировать отчёт
   - Проверить итоги (ручной расчёт)

3. **Тест рекурсии:**
   - ПФ1 → base ингредиенты
   - ПФ2 → ПФ1 + base
   - Проверить себестоимость ПФ2

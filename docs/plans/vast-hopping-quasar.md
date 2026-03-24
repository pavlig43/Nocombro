# План: Реализация расчёта себестоимости для OPZS

## Задача
Реализовать метод `upsertBatchCostFromOpzs` в классе `BatchCostRepository` для расчёта себестоимости готовой продукции при производстве (OPZS).

## Контекст
- **OPZS** — производство продукции из ингредиентов
- Вкладки: **Pf** (готовый продукт) и **Ingredients** (сырьё)
- Расходы (Expenses) для OPZS не используются
- Себестоимость хранится в таблице `batch_cost_price` (коп/кг)

## Файлы для изменения

### 1. `database/src/desktopMain/kotlin/ru/pavlig43/database/data/batch/dao/BatchCostDao.kt`
Добавить метод (его сейчас нет):
```kotlin
@Query("SELECT * FROM batch_cost_price WHERE batch_id = :batchId")
suspend fun getByBatchId(batchId: Int): BatchCostPriceEntity?
```

### 2. `features/form/transaction/src/desktopMain/kotlin/ru/pavlig43/transaction/internal/di/CreateTransactionFormModule.kt`
**Добавить `ingredientDao` в `BatchCostRepository`:**
```kotlin
private val ingredientDao = db.ingredientDao
```

**Реализовать метод `upsertBatchCostFromOpzs`:**
```kotlin
private suspend fun upsertBatchCostFromOpzs(transactionId: Int) {
    // 1. Получить ингредиенты
    val ingredients = ingredientDao.getByTransactionId(transactionId)

    // 2. Получить данные о готовом продукте
    val pf = pfDao.getPf(transactionId) ?: return

    // 3. Рассчитать общую стоимость ингредиентов
    val totalCost = ingredients.sumOf { ingredient ->
        val batchCost = batchCostDao.getByBatchId(ingredient.batchId)
        val costPerKg = batchCost?.costPricePerUnit ?: 0
        // стоимость = (цена_за_кг * граммы) / 1000
        (costPerKg * ingredient.count) / 1000.0
    }

    // 4. Рассчитать себестоимость за кг готовой продукции
    val costPricePerKg = if (pf.count > 0) {
        (totalCost / pf.count.toDouble()) * 1000
    } else {
        0.0
    }

    // 5. Сохранить в batch_cost_price
    val batchCostPriceEntity = BatchCostPriceEntity(
        batchId = pf.batchId,
        costPricePerUnit = costPricePerKg.roundToInt()
    )
    batchCostDao.upsert(listOf(batchCostPriceEntity))
}
```

**Обновить `updateBatchCost`:**
```kotlin
TransactionType.OPZS -> upsertBatchCostFromOpzs(transactionId)
```

## Логика расчёта

```
общая_стоимость_ингредиентов = Σ(себестоимость_кг_ингредиента * количество_граммы / 1000)
себестоимость_кг_готового = (общая_стоимость / количество_готового_граммы) * 1000
```

## Проверка

1. Создать транзакцию OPZS с ингредиентами
2. Указать количество готовой продукции во вкладке Pf
3. Сохранить транзакцию
4. Проверить, что в `batch_cost_price` создана запись с правильной себестоимостью

## Пример

**Входные данные:**
- Ингредиент 1: 500 грамм, себестоимость 100 ₽/кг
- Ингредиент 2: 300 грамм, себестоимость 200 ₽/кг
- Готовый продукт: 800 грамм

**Расчёт:**
- Стоимость ингредиента 1: (100 * 500) / 1000 = 50 копеек
- Стоимость ингредиента 2: (200 * 300) / 1000 = 60 копеек
- Общая стоимость: 50 + 60 = 110 копеек (1.10 ₽)
- Себестоимость готового: (110 / 800) * 1000 = 137.5 коп/кг ≈ 1.38 ₽/кг

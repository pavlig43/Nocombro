# План исправления расчётов в Profitability

## Проблемы

### 1. Integer Overflow (строка 56)
При вычислении `revenue = price * saleQuantity` происходит переполнение `Int`:
```
saleQuantity: 18000 (граммы)
price: 120000 (копеек/грамм)
18000 * 120000 = 2,160,000,000 > Int.MAX_VALUE (2,147,483,647)
Результат: -2134967296
```

### 2. Неверный расчёт costPrice (строка 51)
```kotlin
val costPrice = (batchesCost?.costPricePerUnit ?: 0) * saleQuantity / 1000
```
- `costPricePerUnit` — копеек/грамм
- `saleQuantity` — граммы
- **`/ 1000` — ОШИБКА!** Уменьшает себестоимость в 1000 раз

Поскольку цена указана за грамм, делить на 1000 не нужно.

## Решение

1. **Убрать `/ 1000`** в строке 51 — цена уже за грамм
2. **Использовать `Long`** для умножения во всех местах

## Файлы для изменения

| Файл | Строки |
|------|--------|
| `features/analytic/profitability/src/desktopMain/kotlin/ru/pavlig43/profitability/internal/di/CreateModule.kt` | 51, 56, 108 |

## Изменения

### 1. Строка 51 — costPrice (исправить формулу)
```kotlin
// ДО:
val costPrice = (batchesCost?.costPricePerUnit ?: 0) * saleQuantity / 1000

// ПОСЛЕ:
val costPrice = ((batchesCost?.costPricePerUnit ?: 0).toLong() * saleQuantity).toInt()
```
**Изменения:**
- Убрано `/ 1000` — цена уже за грамм
- Добавлен `toLong()` для защиты от overflow

### 2. Строка 56 — revenue (защита от overflow)
```kotlin
// ДО:
val revenue = sale.sale.price * saleQuantity

// ПОСЛЕ:
val revenue = (sale.sale.price.toLong() * saleQuantity).toInt()
```

### 3. Строка 108 — mainExpenseShare (защита от overflow)
```kotlin
// ДО:
val mainExpenseShare = (totalMainExpenses * product.quantity.value) / totalQuantity

// ПОСЛЕ:
val mainExpenseShare = (totalMainExpenses.toLong() * product.quantity.value / totalQuantity).toInt()
```

## Единицы измерения (для справки)

| Переменная | Единицы |
|------------|---------|
| `costPricePerUnit` | копеек/грамм |
| `sale.sale.price` | копеек/грамм |
| `saleQuantity` | граммы |
| `revenue`, `costPrice`, `expenses` | копейки |

Формула: `копеек/грамм × граммы = копейки`

## Проверка

После изменений:
1. Компиляция успешна
2. `revenue` для 18000г × 120000коп/г = 2,160,000,000 коп = 21,600,000 ₽ ✅
3. `costPrice` считается корректно (без деления на 1000)
4. Отрицательных значений нет

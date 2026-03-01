# План: Исправление логики раскрытия/скрытия партий в Storage

## Описание
Реализовать функционал раскрытия/скрытия партий (batch items) в таблице склада. При клике на продукт должны показываться/скрываться его партии.

## Изменения

### 1. ✅ `StorageProductUi.kt` — уже выполнено
Добавлено поле `isExpanded` для отслеживания состояния раскрытия:
```kotlin
val isExpanded: Boolean = false,
```

### 2. ✅ `StorageComponent.kt` — уже выполнено

**Функция `toggleExpand`** — переключает состояние у продукта:
```kotlin
fun toggleExpand(productId: Int) {
    _products.value = _products.value.map { product ->
        if (product.productId == productId && product.isProduct) {
            product.copy(isExpanded = !product.isExpanded)
        } else {
            product
        }
    }
}
```

**Фильтрация в `tableData`** — партии видны только если родительский продукт раскрыт:
```kotlin
val expandedProductIds = products
    .filter { it.isProduct && it.isExpanded }
    .map { it.productId }
    .toSet()

val filtered = products.filter { item ->
    val matchesFilter = StorageFilterMatcher.matchesItem(item, filters)
    val isVisible = when {
        item.isProduct -> true  // Продукты всегда видимы
        else -> item.productId in expandedProductIds  // Партии только если раскрыт
    }
    matchesFilter && isVisible
}
```

### 3. ❗ `Columns.kt` — нужно исправить
**Проблема:** Используется `item.itemId` вместо `item.productId`

**Исправить строки 77 и 84:**
```kotlin
// Было:
onClick = { onToggleExpand(item.itemId) },

// Должно быть:
onClick = { onToggleExpand(item.productId) },
```

## Почему так работает

| Поле | Продукт | Партия |
|------|---------|--------|
| `isProduct` | `true` | `false` |
| `itemId` | `productId` | `batchId` |
| `productId` | ID продукта | ID родительского продукта |
| `isExpanded` | переключается | не используется |

**Логика:**
1. Клик на продукте → `toggleExpand(productId)` → `isExpanded = !isExpanded`
2. `tableData` пересчитывается через `combine`
3. Фильтр показывает партии только если `isExpanded = true` у родительского продукта

## Файлы для изменения

1. `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/component/Columns.kt` — строки 77, 84

## Проверка

1. Запустить приложение: `./gradlew :app:desktopApp:run`
2. Открыть вкладку "Склад"
3. Кликнуть на стрелку рядом с продуктом
4. Проверить:
   - Стрелка меняет направление (вверх/вниз)
   - Под продуктом появляются партии
   - Повторный клик скрывает партии
5. Проверить фильтры — они должны работать независимо от раскрытия

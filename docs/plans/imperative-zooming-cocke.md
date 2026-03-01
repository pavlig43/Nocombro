# План: Фильтрация по продукту с сохранением партий

## Задача
Фильтрация применяется только к продуктам. Если продукт прошёл фильтр — показываем его и все его партии.

## Решение
Изменить логику фильтрации в `StorageComponent.tableData`

## Изменения

### Файл: `features/storage/src/commonMain/kotlin/ru/pavlig43/storage/api/component/StorageComponent.kt`

**Строки 61-68** — изменить логику фильтрации:

```kotlin
// Было:
val filtered = products.filter { item ->
    StorageFilterMatcher.matchesItem(item, filters)
}

// Станет:
// 1. Фильтруем только продукты (isProduct == true)
val filteredProductIds = products
    .filter { it.isProduct && StorageFilterMatcher.matchesItem(it, filters) }
    .map { it.productId }
    .toSet()

// 2. Оставляем продукты + их партии (по productId)
val filtered = products.filter { it.productId in filteredProductIds }
```

## Проверка
1. Ввести текст в фильтр по имени — продукт и его партии остаются
2. Сбросить фильтр — всё возвращается

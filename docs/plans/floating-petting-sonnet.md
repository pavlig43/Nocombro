# План: Исправление фильтрации для DecimalData2/DecimalData3

## Проблема

Фильтр суммы **не позволяет ввести точку** — при попытке ввести `1.` точка исчезает.

---

## Анализ проблемы

### Что происходит:

1. Пользователь вводит `"1."`
2. Regex пропускает → `editingText = "1."`
3. Через 300ms срабатывает autoApply
4. `parse("1.")` → `"1.".toDoubleOrNull()` = `1.0` → `DecimalData2(100)`
5. `onStateChange` → `externalState.values = [DecimalData2(100)]`
6. `sourceText` обновляется через `?.toString()` → получает `"1"`
7. `LaunchedEffect` синхронизирует `editingText = sourceText` → `"1"` (точка исчезает!)

### Корневая причина

В библиотеке `ua.wwind.table` в `rememberNumberFilterState`:

```kotlin
val sourceText by remember(externalState) {
    derivedStateOf {
        externalState?.values?.firstOrNull()?.toString() ?: ""  // ← использует toString()
    }
}
```

Библиотека использует `?.toString()` вместо `filter.delegate.format()`.

---

## Решение

**Пользователь сам напишет issue разработчикам библиотеки `ua.wwind.table`**

---

## Что уже сделано

✅ Regex исправлен (`\\d` вместо `\\\\d`)
✅ `toString()` переопределён для возврата `toStartDoubleFormat()`

Но проблема с точкой остаётся.

---

## План действий

1. **Найти репозиторий** библиотеки `ua.wwind.table` для форка
2. **Форкнуть и клонировать**
3. **Исправить** `NumberFilterState.kt` — использовать `filter.delegate.format()` вместо `toString()`
4. **Подключить локально** через composite build
5. **Протестировать**
6. **Написать issue** с pull request

---

## Файлы

**`features/table/core/src/desktopMain/kotlin/ru/pavlig43/tablecore/utils/FilterMatcher.kt`**
- `DataDecimalDelegate2` (строка 176)
- `DataDecimalDelegate3` (строка 202)

**`core/src/desktopMain/kotlin/ru/pavlig43/core/model/DecimalData.kt`**
- `DecimalData2.toString()` (строка 14)
- `DecimalData3.toString()` (строка 34)

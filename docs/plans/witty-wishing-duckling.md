# План: Исправление двойной обработки ESC при редактировании ячейки таблицы

## Проблема

При редактировании ячейки таблицы ESC выполняет два действия:
1. Таблица (`ua.wwind.table-kmp`) перехватывает ESC `KeyDown` через `onPreviewKeyEvent` → `cancelEditing()`
2. Window `onKeyEvent` ловит ESC `KeyUp` → `backDispatcher.back()` → закрытие вкладки

Это **разные цепочки событий**: Compose-модификаторы и Window-level callback — между ними нет координации.

## Решение

**Перенести обработку ESC из `Window.onKeyEvent` в `Modifier.onKeyEvent` (post-dispatch) в корне Compose-дерева.**

Ключевое свойство: если `onPreviewKeyEvent` (pre-dispatch) в Compose-дереве возвращает `true` (поглощает событие), то `onKeyEvent` (post-dispatch) **не вызывается**. Это позволяет детектировать, что таблица поглотила ESC `KeyDown`.

### Логика

- Флаг `escKeyDownConsumed` по умолчанию `true` (пессимистичный: считаем что событие поглощено)
- ESC `KeyDown` доходит до нашего `onKeyEvent` → значит **никто** в pre-dispatch не поглотил → флаг = `false`
- ESC `KeyDown` НЕ доходит (post-dispatch skipped) → кто-то поглотил (таблица) → флаг остаётся `true`
- ESC `KeyUp` → проверяем флаг: `true` = подавить, `false` = `backDispatcher.back()`

### Сценарии

| Сценарий | KeyDown | KeyUp | Результат |
|----------|---------|-------|-----------|
| ESC в таблице | Поглощён таблицей (флаг=true) | Флаг=true → подавить | Только отмена редактирования |
| Обычный ESC | Дошёл до нас (флаг=false) | Флаг=false → back() | Закрытие вкладки/диалога |
| ESC при открытом диалоге | Дошёл (флаг=false) | back() → BackCallback диалога | Закрытие диалога |
| Двойной ESC в таблице | 1й поглощён, 2й доходит | 1й подавлен, 2й back() | 1й: отмена, 2й: закрытие вкладки |

## Файлы для изменения

### 1. `app/desktopApp/src/desktopMain/kotlin/ru/pavlig43/nocombro/Main.kt`

- Убрать `onKeyEvent` из `Window`
- Добавить `Modifier.onKeyEvent` на корневой `Box`, оборачивающий `App()`:
  - ESC `KeyDown` → `escKeyDownConsumed = false` + `KeyEventHandler.handle(event)`
  - ESC `KeyUp` → если флаг `true`: подавить; если `false`: `backDispatcher.back()`
  - Остальные события → `KeyEventHandler.handle(event)`

```kotlin
Window(
    onCloseRequest = ::exitApplication,
    title = "Nocombro",
    state = windowState,
    // УБРАН onKeyEvent
) {
    LifecycleController(...)

    var escKeyDownConsumed by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.onKeyEvent { event ->
            when {
                event.key == Key.Escape && event.type == KeyEventType.KeyDown -> {
                    escKeyDownConsumed = false
                    KeyEventHandler.handle(event)
                }
                event.key == Key.Escape && event.type == KeyEventType.KeyUp -> {
                    if (escKeyDownConsumed) {
                        escKeyDownConsumed = true
                        true
                    } else {
                        escKeyDownConsumed = true
                        backDispatcher.back()
                        true
                    }
                }
                else -> KeyEventHandler.handle(event)
            }
        }
    ) {
        App(rootNocombroComponent)
    }
}
```

### 2. `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/ui/RootNocombroScreen.kt`

- Убрать отладочные `println("Drawer is open")` и `println("Drawer is closed")`

## Верификация

1. Открыть таблицу, кликнуть на ячейку для редактирования → нажать ESC → ячейка должна выйти из редактирования, вкладка НЕ должна закрыться
2. Не редактируя ячейку → нажать ESC → вкладка должна закрыться (или диалог, если открыт)
3. Открыть диалог (например, DatePicker) → нажать ESC → диалог должен закрыться, вкладка НЕ должна закрыться
4. Редактировать ячейку → ESC → ESC → первое нажатие отменяет редактирование, второе закрывает вкладку
5. Открытый drawer → ESC → drawer закрывается

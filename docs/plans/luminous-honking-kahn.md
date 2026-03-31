# План: ESC не закрывает drawer, а закрывает вкладку

## Проблема

`drawerState.isOpen` = `currentValue == DrawerValue.Open`. Во время анимации открытия `currentValue` ещё `Closed`, поэтому ESC видит drawer как закрытый и закрывает вкладку.

## Исправление

**Файл:** `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/api/ui/RootNocombroScreen.kt:80-91`

Заменить `drawerState.isOpen` на `drawerState.targetValue == DrawerValue.Open`, убрать `println`:

```kotlin
BackHandler(
    tabNavigationComponent.backHandler,
    onBack = {
        if (drawerState.targetValue == DrawerValue.Open) {
            coroutineScope.launch { drawerState.close() }
        } else {
            tabNavigationComponent.onCloseCurrentTab()
        }
    },
)
```

## Верификация

1. Открыть drawer → ESC до завершения анимации → drawer закрывается, вкладка остаётся
2. Открыть drawer → подождать → ESC → drawer закрывается
3. Нет открытых диалогов/drawer → ESC → закрывается вкладка

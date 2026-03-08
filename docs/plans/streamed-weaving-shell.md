# План исправления ProductDeclarationComponent

## Описание проблемы

В файле `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/update/tabs/declaration/ProductDeclarationComponent.kt`:

1. **Строка 116**: Неправильный тип `tableData` — указан `StateFlow<ProductDeclarationIn>`, должен быть `StateFlow<TableData<FlowProductDeclarationTableUi>>`
2. **Строка 141**: Используется неопределённая переменная `uiList` в `errorMessages`
3. **Отсутствует функция `onEvent`** — нужна для обработки событий таблицы
4. **Неправильный репозиторий** — используется `ProductDeclarationRepository1` вместо полного `FlowMultilineRepository`

## Критические файлы

- `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/update/tabs/declaration/ProductDeclarationComponent.kt`
- `features/form/product/src/commonMain/kotlin/ru/pavlig43/product/internal/di/ProductFormModule.kt`

## План исправления

### Шаг 1. Исправить DI в ProductFormModule.kt

Заменить `ProductDeclarationRepository1` на использование полного `FlowMultilineRepository<ProductDeclarationOut, ProductDeclarationIn>` (уже зарегистрирован как single, строка 39).

### Шаг 2. Добавить uiList в ProductDeclarationComponent

Добавить StateFlow с UI-данными:

```kotlin
internal val uiList: StateFlow<List<FlowProductDeclarationTableUi>> = productDeclarations
    .map { declarations ->
        val declarationIds = declarations.map { it.declarationId }
        observableRepository.observeOnItemsByIds(declarationIds)
            .map { result ->
                result.getOrElse { emptyList() }
                    .mapIndexed { index, dec -> dec.toUi(index) }
            }
    }
    .flatMapLatest { it }
    .stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        emptyList()
    )
```

### Шаг 3. Исправить тип tableData

```kotlin
internal val tableData: StateFlow<TableData<FlowProductDeclarationTableUi>> = combine(
    uiList,
    selectionManager.selectedIdsFlow,
) { declarations, selectedIds ->
    TableData(
        displayedItems = declarations,
        selectedIds = selectedIds,
        isSelectionMode = true
    )
}.stateIn(
    coroutineScope,
    SharingStarted.Eagerly,
    TableData(isSelectionMode = true)
)
```

### Шаг 4. Исправить errorMessages

Заменить `uiList.map` на правильный source:

```kotlin
override val errorMessages: Flow<List<String>> = uiList.map { lst ->
    // ...
}
```

### Шаг 5. Добавить функцию onEvent (если нужна для таблицы)

```kotlin
fun onEvent(event: MutableUiEvent) {
    when (event) {
        is MutableUiEvent.Selection -> {
            selectionManager.onEvent(event.selectionUiEvent)
        }
        // другие события...
    }
}
```

### Шаг 6. Обновить импорты

Добавить необходимые импорты:
- `import kotlinx.coroutines.flow.flatMapLatest`
- `import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent`

## Проверка

После исправления:
1. Проверить, что `tableData` имеет правильный тип
2. Проверить, что `uiList` определён и используется в `errorMessages`
3. Проверить, что функция `onEvent` определена (если нужна)
4. Убедиться, что все импорты корректны

# Plan: Исправить описание ImmutableTableComponent в CLAUDE.md

## Задача
Исправить неверное описание `ImmutableTableComponent` в секции "#### 3. Immutable Tables" файла `CLAUDE.md`.

## Что изменить

### Текущее (неверное) описание:
```kotlin
internal class MyComponent : ImmutableTableComponent<Item> {
    override val columns: ImmutableList<ColumnSpec<Item, *, Unit>>
    override val tableData: StateFlow<List<Item>>
}
```

### Правильное описание:
```kotlin
internal abstract class ImmutableTableComponent<BD, UI : IMultiLineTableUi, Column>(
    componentContext: ComponentContext,
    tableBuilder: ImmutableTableBuilderData<UI>,
    val onCreate: () -> Unit,
    val onItemClick: (UI) -> Unit,
    mapper: BD.() -> UI,
    filterMatcher: FilterMatcher<UI, Column>,
    sortMatcher: SortMatcher<UI, Column>,
    val repository: ImmutableListRepository<BD>,
) : ComponentContext by componentContext {

    abstract val columns: ImmutableList<ColumnSpec<UI, Column, TableData<UI>>>
    val tableData: StateFlow<TableData<UI>>
    val itemListState: StateFlow<ItemListState<UI>>
}
```

## Ключевые отличия

1. **Abstract class** (не interface)
2. **3 типовых параметра**: `<BD, UI : IMultiLineTableUi, Column>`
   - `BD` — тип данных из БД
   - `UI` — тип для отображения (наследует `IMultiLineTableUi`)
   - `Column` — тип ключей колонок
3. **`columns`**: `ColumnSpec<UI, Column, TableData<UI>>` (не `ColumnSpec<Item, *, Unit>`)
4. **`tableData`**: `StateFlow<TableData<UI>>` (не `StateFlow<List<Item>>`)
5. Дополнительные свойства и методы:
   - `itemListState: StateFlow<ItemListState<UI>>` — состояние загрузки (Loading/Success/Error)
   - `onEvent(event: ImmutableTableUiEvent)` — обработка событий
   - `updateFilters()`, `updateSort()` — управление фильтрацией и сортировкой

## Файл для изменения

`C:\Users\user\AndroidStudioProjects\Nocombro\CLAUDE.md`

Секция: `#### 3. Immutable Tables (features/table/immutable)`

## Проверка

После изменения убедиться, что описание соответствует реальному интерфейсу в файле:
`features/table/immutable/src/commonMain/kotlin/ru/pavlig43/immutable/internal/component/ImmutableTableComponent.kt:33-131`

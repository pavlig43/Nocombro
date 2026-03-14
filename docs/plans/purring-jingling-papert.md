# План: Реализовать onItemClick и onCreate в ImmutableTableComponentFactoryMain

## Задача

В `ImmutableTableComponentFactoryMain.kt` для `DocumentTableComponent` уже реализованы `onItemClick` и `onCreate` как лямбды прямо в фабрике. Нужно сделать так же для всех остальных `ImmutableTableComponent`.

## Текущее состояние

**DocumentTableComponent** (уже сделано):
```kotlin
is DocumentImmutableTableBuilder -> DocumentTableComponent(
    componentContext = context,
    tableBuilder = immutableTableBuilderData,
    onItemClick = { tabOpener.openDocumentTab(it.composeId) },
    onCreate = {tabOpener.openDocumentTab(0)},
    repository = ...
)
```

**DeclarationTableComponent** (нужно изменить):
```kotlin
is DeclarationImmutableTableBuilder -> DeclarationTableComponent(
    componentContext = context,
    tableBuilder = immutableTableBuilderData,
    onItemClick = onItemClick,           // ❌ передаётся параметр
    tabOpener = tabOpener,               // ❌ передаётся параметр
    repository = ...
)
```

## Изменения в ImmutableTableComponentFactoryMain.kt

Заменить для всех компонентов (кроме Document):

| Компонент | onItemClick | onCreate |
|-----------|-------------|----------|
| Declaration | `{ tabOpener.openDeclarationTab(it.composeId) }` | `{ tabOpener.openDeclarationTab(0) }` |
| Product | `{ tabOpener.openProductTab(it.composeId) }` | `{ tabOpener.openProductTab(0) }` |
| Transaction | `{ tabOpener.openTransactionTab(it.composeId) }` | `{ tabOpener.openTransactionTab(0) }` |
| Vendor | `{ tabOpener.openVendorTab(it.composeId) }` | `{ tabOpener.openVendorTab(0) }` |
| ProductDeclaration | `{ tabOpener.openDeclarationTab(it.composeId) }` | `{ tabOpener.openDeclarationTab(0) }` |
| Batch | `{ tabOpener.openTransactionTab(it.composeId) }` | `{ tabOpener.openTransactionTab(0) }` |
| Safety | `{ tabOpener.openProductTab(it.composeId) }` | `{ tabOpener.openProductTab(0) }` |
| Expense | `{ tabOpener.openExpenseFormTab(it.composeId) }` | `{ tabOpener.openExpenseFormTab(0) }` |

## Изменения в конструкторах компонентов

Заменить `tabOpener: TabOpener` на `onCreate: () -> Unit`

## Файлы для изменения

1. `features/table/immutable/src/desktopMain/kotlin/ru/pavlig43/immutable/api/component/ImmutableTableComponentFactoryMain.kt`
2. Все `XXXTableComponent.kt` (кроме Document):
   - DeclarationTableComponent.kt
   - ProductTableComponent.kt
   - TransactionTableComponent.kt
   - VendorTableComponent.kt
   - ProductDeclarationTableComponent.kt
   - BatchTableComponent.kt
   - SafetyTableComponent.kt
   - ExpenseTableComponent.kt

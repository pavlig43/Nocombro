# План: Добавить опциональный onCreate в immutable компоненты

## Задача
Компоненты должны поддерживать два сценария:
1. **Таблица на вкладке** → использует `tabOpener.open...Tab(0)`
2. **MBSImmutableTableComponent (диалог)** → использует переданный `onCreate`

## Решение
Добавить опциональный параметр `onCreate` в компоненты. Если передан — использовать его, иначе использовать `tabOpener`.

## Файлы для изменения

### 1. ImmutableTableComponentFactoryMain
Добавить опциональный `onCreate`:
```kotlin
private val onCreate: (() -> Unit)? = null,
```

### 2. Конкретные компоненты (10 файлов)
Добавить опциональный `onCreate` и использовать его вместо `tabOpener`:

- `VendorTableComponent.kt`
- `ProductTableComponent.kt`
- `DeclarationTableComponent.kt`
- `DocumentTableComponent.kt`
- `TransactionTableComponent.kt`
- `ProductDeclarationTableComponent.kt`
- `BatchTableComponent.kt`
- `SafetyTableComponent.kt`
- `ExpenseTableComponent.kt`

Изменение (пример для Vendor):
```kotlin
internal class VendorTableComponent(
    componentContext: ComponentContext,
    tableBuilder: VendorImmutableTableBuilder,
    tabOpener: TabOpener,
    onCreate: (() -> Unit)? = null,  // добавить
    onItemClick: (VendorTableUi) -> Unit,
    repository: ImmutableListRepository<Vendor>,
) : ImmutableTableComponent<Vendor, VendorTableUi, VendorField>(
    ...
    onCreate = onCreate ?: { tabOpener.openVendorTab(0) },  // изменить
    ...
)
```

### 3. ImmutableTableComponentFactoryMain.build()
Передавать `onCreate` в компоненты:
```kotlin
VendorTableComponent(
    ...
    tabOpener = tabOpener,
    onCreate = onCreate,  // добавить
    ...
)
```

## Проверка
После изменений:
- `MBSImmutableTableComponent` может передать свой `onCreate`
- Таблицы на вкладках продолжают использовать `tabOpener`

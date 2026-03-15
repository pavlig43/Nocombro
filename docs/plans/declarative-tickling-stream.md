# План исправления формы расходов (Expense Form)

## Проблема

Ошибка: "Cannot access 'MutableTableComponent' which is a supertype of 'ExpenseFormComponent'"

## Причина

`MutableTableComponent` находится в `mutable.api.multiLine.component`, но все компоненты, которые от него наследуются, находятся в `internal` пакете своих модулей:

- `transaction.internal.update.tabs.component.expenses.ExpensesComponent` ✅
- `transaction.internal.update.tabs.component.sale.SaleComponent` ✅
- `transaction.internal.update.tabs.component.buy.BuyComponent` ✅

Наш `ExpenseFormComponent` находится в `api.component` (публичный API) - это проблема видимости!

## Решение: Разделить на 2 компонента

### Структура:

```
api/
└── component/
    ├── ExpenseFormComponent.kt (api) - обёртка, MainTabComponent
    └── ExpenseFormDependencies.kt

internal/
└── component/
    └── ExpenseStandaloneComponent.kt - MutableTableComponent
```

### Шаг 1: Создать ExpenseStandaloneComponent (internal)

**Файл:** `features/form/expense/src/desktopMain/kotlin/ru/pavlig43/expense/internal/component/ExpenseStandaloneComponent.kt`

```kotlin
package ru.pavlig43.expense.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.mutable.api.multiLine.component.MutableTableComponent
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.expense.api.model.ExpenseStandaloneUi
import ru.pavlig43.expense.api.model.ExpenseStandaloneField

internal class ExpenseStandaloneComponent(
    componentContext: ComponentContext,
    repository: UpdateCollectionRepository<ExpenseBD, ExpenseBD>,
) : MutableTableComponent<ExpenseBD, ExpenseBD, ExpenseStandaloneUi, ExpenseStandaloneField>(
    componentContext = componentContext,
    parentId = 0,
    title = "Расходы",
    sortMatcher = ExpenseStandaloneSorter,
    filterMatcher = ExpenseStandaloneFilterMatcher,
    repository = repository
) {
    // Вся логика таблицы...
}
```

### Шаг 2: Создать ExpenseFormComponent (api)

**Файл:** `features/form/expense/src/desktopMain/kotlin/ru/pavlig43/expense/api/component/ExpenseFormComponent.kt`

```kotlin
package ru.pavlig43.expense.api.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.expense.api.ExpenseFormDependencies
import ru.pavlig43.expense.internal.component.ExpenseStandaloneComponent

class ExpenseFormComponent(
    componentContext: ComponentContext,
    dependencies: ExpenseFormDependencies,
) : ComponentContext by componentContext, MainTabComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope = koinContext.getOrCreateKoinScope(createExpenseFormModule(dependencies))

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Расходы"))
    override val model = _model.asStateFlow()

    val expenseStandaloneComponent = ExpenseStandaloneComponent(
        componentContext = componentContext,
        repository = scope.get()
    )
}
```

### Шаг 3: Обновить навигацию

**MainTabNavigationComponent.kt:**
```kotlin
import ru.pavlig43.expense.api.component.ExpenseFormComponent
```

**MainTabChild.kt:**
```kotlin
import ru.pavlig43.expense.api.component.ExpenseFormComponent

sealed interface ItemFormChild: MainTabChild {
    // ...
    class ExpenseFormChild(override val component: ExpenseFormComponent): ItemFormChild
}
```

## Критические файлы для изменения

| Файл | Действие |
|------|----------|
| `internal/component/ExpenseStandaloneComponent.kt` | СОЗДАТЬ (с логикой таблицы) |
| `api/component/ExpenseFormComponent.kt` | ИЗМЕНИТЬ (обёртка, без MutableTableComponent) |
| `MainTabNavigationComponent.kt` | Обновить импорты |

## Проверка

1. `./gradlew build` - успешно
2. Запуск desktop - работает
3. Форма расходов открывается


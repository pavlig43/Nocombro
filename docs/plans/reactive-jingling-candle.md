# План: Добавление вкладки "Файлы" в форму расхода (Expense)

## Обзор
Добавить вкладку с файлами в форму расхода по аналогии с формой документа (Document).

## Анализ текущей структуры

**Существующая структура в Document:**
- `DocumentFilesComponent` - наследует `FilesComponent` с `OwnerType.DOCUMENT`
- `DocumentTab.Files` - вкладка в sealed interface
- `DocumentTabChild.Files` - дочерний компонент
- `DocumentFormTabsComponent` - создание вкладки через `tabChildFactory`
- `DocumentFormScreen` - отображение через `FilesScreen`
- `DocumentFormDependencies` - содержит `filesDependencies: FilesDependencies`

**Текущая структура в Expense:**
- `ExpenseTab` - enum class с `Expenses` (закомментирован `Files`)
- `ExpenseTabChild` - sealed interface только с `Expenses`
- `ExpenseFormTabsComponent` - без поддержки вкладки файлов
- `ExpenseFormDependencies` - без `filesDependencies`

## Файлы для создания/изменения

### 1. Database Layer
**Файл:** `database/src/desktopMain/kotlin/ru/pavlig43/database/data/files/OwnerType.kt`
- Добавить `EXPENSE` в enum

### 2. API Layer (Dependencies)
**Файл:** `features/form/expense/src/desktopMain/kotlin/ru/pavlig43/expense/api/ExpenseFormDependencies.kt`

Добавить импорт:
```kotlin
import ru.pavlig43.files.api.FilesDependencies
```

Добавить поле:
```kotlin
val filesDependencies: FilesDependencies
```

### 3. DI Layer
**Файл:** `features/form/expense/src/desktopMain/kotlin/ru/pavlig43/expense/internal/di/ExpenseFormModule.kt`

Добавить импорт:
```kotlin
import ru.pavlig43.files.api.FilesDependencies
```

Добавить регистрацию в module:
```kotlin
single<FilesDependencies> { dependencies.filesDependencies }
```

### 4. Component Layer (Tab)
**Файл:** `features/form/expense/src/desktopMain/kotlin/ru/pavlig43/expense/internal/component/ExpenseTab.kt`
- Раскомментировать `Files` в enum (или переделать на sealed interface)

### 5. Component Layer (TabChild)
**Файл:** `features/form/expense/src/desktopMain/kotlin/ru/pavlig43/expense/internal/component/ExpenseTabChild.kt`

Добавить импорт:
```kotlin
import ru.pavlig43.expense.internal.component.tabs.files.ExpenseFilesComponent
```

Добавить класс:
```kotlin
class Files(override val component: ExpenseFilesComponent) : ExpenseTabChild
```

### 6. Component Layer (FilesComponent)
**Новый файл:** `features/form/expense/src/desktopMain/kotlin/ru/pavlig43/expense/internal/component/tabs/files/ExpenseFilesComponent.kt`
```kotlin
package ru.pavlig43.expense.internal.component.tabs.files

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.files.api.component.FilesComponent

internal class ExpenseFilesComponent(
    componentContext: ComponentContext,
    expenseId: Int,
    dependencies: FilesDependencies,
) : FilesComponent(
    componentContext = componentContext,
    ownerId = expenseId,
    ownerType = OwnerType.EXPENSE,
    dependencies = dependencies
) {
    override val errorMessages: Flow<List<String>> = isAllFilesUpload.map { isUpload ->
        buildList {
            if (!isUpload) add("Идет загрузка")
        }
    }
}
```

### 7. Component Layer (TabsComponent)
**Файл:** `features/form/expense/src/desktopMain/kotlin/ru/pavlig43/expense/internal/component/ExpenseFormTabsComponent.kt`

Добавить импорты:
```kotlin
import ru.pavlig43.expense.internal.component.tabs.files.ExpenseFilesComponent
import ru.pavlig43.expense.internal.component.ExpenseTabChild.Files
```

В `startConfigurations` добавить:
```kotlin
startConfigurations = listOf(
    ExpenseTab.Expenses,
    ExpenseTab.Files
)
```

В `tabChildFactory` добавить обработку:
```kotlin
when (tabConfig) {
    ExpenseTab.Expenses -> ExpenseTabChild.Expenses(
        TableComponent(
            componentContext = context,
            repository = scope.get(),
            expenseId = expenseId
        )
    )
    ExpenseTab.Files -> Files(
        ExpenseFilesComponent(
            componentContext = context,
            expenseId = expenseId,
            dependencies = scope.get()
        )
    )
}
```

### 8. UI Layer
**Файл:** `features/form/expense/src/desktopMain/kotlin/ru/pavlig43/expense/api/ui/ExpenseStandaloneScreen.kt`

Добавить импорты:
```kotlin
import ru.pavlig43.files.api.ui.FilesScreen
```

Обновить `tabChildFactory`:
```kotlin
tabChildFactory = { tabChild ->
    when (tabChild) {
        is ExpenseTabChild.Expenses -> TableScreen(tabChild.component)
        is ExpenseTabChild.Files -> FilesScreen(tabChild.component)
        null -> Unit
    }
}
```

### 9. Root DI (если нужно)
**Файл:** `rootnocombro/src/desktopMain/kotlin/ru/pavlig43/rootnocombro/internal/di/RootNocombroModule.kt`
- Проверить, что `FilesDependencies` зарегистрирован (уже должен быть)

## Порядок реализации

1. **Database**: Добавить `EXPENSE` в `OwnerType`
2. **API Dependencies**: Добавить `filesDependencies` в `ExpenseFormDependencies`
3. **DI**: Зарегистрировать `FilesDependencies` в `ExpenseFormModule`
4. **Component**: Создать `ExpenseFilesComponent`
5. **Tabs**: Обновить `ExpenseTab` и `ExpenseTabChild`
6. **TabsComponent**: Обновить `ExpenseFormTabsComponent`
7. **UI**: Обновить `ExpenseStandaloneScreen`

## Проверка

### Компиляция
```bash
./gradlew :features:form:expense:compileDesktopMainKotlinMetadata
```

### Функциональная проверка
1. Открыть форму расхода
2. Переключиться на вкладку "Файлы"
3. Добавить файл через кнопку "+"
4. Проверить, что файл отображается и загружается
5. Сохранить форму и проверить, что файл сохраняется
6. Переоткрыть форму и проверить, что файл загружается из БД

## Критические файлы

| Файл | Действие |
|------|----------|
| `OwnerType.kt` | Добавить enum value |
| `ExpenseFormDependencies.kt` | Добавить поле |
| `ExpenseFormModule.kt` | Регистрация DI |
| `ExpenseTab.kt` | Раскомментировать |
| `ExpenseTabChild.kt` | Добавить case |
| `ExpenseFilesComponent.kt` | Создать новый |
| `ExpenseFormTabsComponent.kt` | Добавить вкладку |
| `ExpenseStandaloneScreen.kt` | Добавить UI |

## Заметки

- Паттерн валидации выбран как в Vendor/Product (только проверка загрузки)
- Если нужно требовать хотя бы один файл - использовать паттерн из Document
- Готовый `FilesScreen` из `ru.pavlig43.files.api.ui` переиспользуется без изменений

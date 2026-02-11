# Refactoring Declaration Module

## Обзор

Рефакторинг модуля declaration для унификации структуры с другими form модулями (document, product, vendor).

## Изменения структуры

### Было (старая структура)
```
internal/
├── component/
│   ├── CreateDeclarationComponent.kt
│   ├── VendorDialogComponent.kt
│   └── tabs/
│       ├── DeclarationFormTabsComponent.kt
│       ├── DeclarationTab.kt
│       ├── DeclarationTabChild.kt
│       └── component/
│           ├── DeclarationEssentialComponent.kt
│           ├── DeclarationFilesComponent.kt
│           └── ...
├── data/
│   └── DeclarationEssentialsUi.kt
└── ui/
    ├── CreateDeclarationScreen.kt
    └── DeclarationFields.kt
```

### Стало (новая структура)
```
internal/
├── create/
│   └── component/
│       ├── CreateDeclarationSingleLineComponent.kt
│       └── Column.kt
├── update/
│   ├── DeclarationFormTabsComponent.kt
│   ├── DeclarationTab.kt
│   ├── DeclarationTabChild.kt
│   └── tabs/
│       ├── essential/
│       │   ├── DeclarationUpdateSingleLineComponent.kt
│       │   ├── Column.kt
│       │   └── UpdateDeclarationSingleLineScreen.kt
│       └── component/
│           └── DeclarationFilesComponent.kt
├── model/
│   └── DeclarationEssentialsUi.kt
├── DeclarationField.kt
```

## Ключевые изменения

### 1. Разделение Create и Update

**Было:**
- `CreateDeclarationComponent.kt` - общий компонент создания
- `DeclarationEssentialComponent.kt` - использовался и для create, и для update
- Логика create/update была смешана в одних файлах

**Стало:**
- `create/component/CreateDeclarationSingleLineComponent.kt` - только для создания
- `update/tabs/essential/DeclarationUpdateSingleLineComponent.kt` - только для редактирования
- Чёткое разделение ответственности

### 2. Вынос моделей

**Было:**
```
internal/data/DeclarationEssentialsUi.kt
```

**Стало:**
```
internal/model/DeclarationEssentialsUi.kt
```

Более понятное название для UI моделей.

### 3. Унификация с Document модулем

Теперь структура declaration соответствует структуре document:

| Declaration | Document |
|-------------|-----------|
| `create/component/CreateDeclarationSingleLineComponent.kt` | `create/component/CreateDocumentSingleLineComponent.kt` |
| `update/tabs/essential/DeclarationUpdateSingleLineComponent.kt` | `update/tabs/essential/DocumentUpdateSingleLineComponent.kt` |
| `DeclarationFormTabsComponent.kt` | `DocumentFormTabsComponent.kt` |

### 4. Изменения в компонентах

#### CreateDeclarationSingleLineComponent
- Использует `CreateSingleLineComponent` как базовый класс
- Содержит dialog для vendor, born date, best before date
- Принимает `immutableDependencies` и `tabOpener` напрямую (не scope)

#### DeclarationUpdateSingleLineComponent
- Использует `UpdateSingleLineComponent` как базовый класс
- Добавлена структура диалогов (как в create версии):
  - `UpdateDialogConfig` - конфигурации диалогов
  - `UpdateDialogChild` - типы дочерних компонентов
- Принимает `immutableDependencies` напрямую (не scope)
- Теперь можно редактировать vendor в update версии!

#### Column.kt (update)
- Заменён `readTextColumn` на `textWithSearchIconColumn` для vendor
- Добавлен параметр `onOpenVendorDialog`

## Зачем это нужно?

1. **Последовательность** - все form модули имеют одинаковую структуру
2. **Поддерживаемость** - проще найти нужный файл (create или update)
3. **Чёткая ответственность** - create компоненты не зависят от update логики и наоборот

## Следующие шаги для других модулей

Такой же паттерн можно применить к:
- **transaction** - если нужно

## Зависимости

- `ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent`
- `ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent`
- `ru.pavlig43.immutable.api.ImmutableTableDependencies`
- `ru.pavlig43.core.tabs.TabOpener`

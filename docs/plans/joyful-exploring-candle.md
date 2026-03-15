# План: Исправление ошибки detekt с NullPointerException

## Описание

Detekt версии 1.23.8 падает с `NullPointerException` в правиле `IgnoredReturnValue` при анализе некоторых файлов:
- `database/src/desktopMain/kotlin/ru/pavlig43/database/NocombroDatabase.kt`
- `features/storage/src/desktopMain/kotlin/ru/pavlig43/storage/api/component/batchMovement/BatchMovementComponent.kt`

Это известный баг detekt на определённых конструкциях Kotlin (вложенные лямбды).

## Решение

Отключить правило `IgnoredReturnValue` в конфигурации detekt.

## Шаги выполнения

### 1. Изменить конфиг detekt

**Файл:** `default-detekt-config.yml`

**Изменение:** Установить `active: false` для правила `IgnoredReturnValue` (строки 452-467)

```yaml
IgnoredReturnValue:
  active: false  # Было: true, меняем на false из-за NPE в detekt 1.23.8
  restrictToConfig: true
  # ... остальная конфигурация ...
```

### 2. Проверка

Запустить detekt снова:
```bash
./gradlew detektDesktopMain --continue
```

## Критические файлы

- `default-detekt-config.yml` — конфигурация detekt (строки 452-467)

## Верификация

После применения изменений:
1. Detekt должен завершиться без NullPointerException
2. Сборка должна пройти успешно (или показать другие проблемы, если они есть)

## Примечание

Правило `IgnoredReturnValue` проверяет, что возвращаемые значения функций не игнорируются.
Отключение этого правила означает, что detekt больше не будет предупреждать о потенциально
пропущенных возвращаемых значениях.

Если в будущем будет нужно это правило, можно:
1. Обновить detekt до более новой версии (может быть исправлено)
2. Добавить `@Suppress("IgnoredReturnValue")` на конкретные функции

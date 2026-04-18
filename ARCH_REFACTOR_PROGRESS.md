# Architecture Refactor Progress

Последнее обновление: 2026-04-18

Этот файл нужен как переносимая точка продолжения работы. Если открыть проект на другом компьютере или в новой сессии агента, начинать лучше с него, а потом уже смотреть связанные файлы и анализ.

## Цель

Постепенно уменьшить связанность проекта без большого одномоментного рефакторинга:
- убрать скрытые транзитивные зависимости
- сделать compile-time зависимости явными
- ослабить знание `rootnocombro` о concrete feature-реализациях
- подготовить почву для выделения более узких capability/API модулей

Базовый анализ сохранен в:
- `Aalekh-architecture-analysis.md`

## Текущий статус

Работа начата.

Сделано:
- прочитан `aalekhReport` и зафиксирован архитектурный анализ в `Aalekh-architecture-analysis.md`
- выделен безопасный первый шаг: уменьшение скрытой связанности через Gradle dependencies
- в `features/table/immutable/build.gradle.kts` зависимость на `projects.features.table.core` изменена с `api(...)` на `implementation(...)`
- в `features/table/mutable/build.gradle.kts` зависимость на `projects.features.table.core` изменена с `api(...)` на `implementation(...)`
- в `rootnocombro/build.gradle.kts` добавлена явная зависимость `implementation(projects.features.table.core)`, потому что `rootnocombro` напрямую использует типы из `ru.pavlig43.tablecore.*`
- при проверке сборки обнаружено, что `features:form:declaration` тоже использовал `table:core` транзитивно через `table:immutable`
- в `features/form/declaration/build.gradle.kts` добавлена явная зависимость `implementation(projects.features.table.core)`

Почему это полезно:
- `rootnocombro` теперь зависит от `table:core` явно, а не через скрытый re-export из `table:immutable`
- граф зависимостей становится честнее
- это уменьшает "магическую" связанность и упрощает дальнейшее ужесточение границ модулей

## Что проверить после этого шага

Нужно прогнать сборку модулей-потребителей `table:immutable` / `table:mutable` и убедиться, что нигде не было неявной зависимости на `table:core`.

Приоритетные проверки:
- `:rootnocombro`
- `:features:sampletable`
- `:features:storage`
- `:features:analytic:profitability`
- `:features:form:product`
- `:features:form:transaction`

Если что-то упадет:
- не возвращать `api(...)` сразу
- сначала добавить явную зависимость в конкретный модуль-потребитель, если он реально использует `ru.pavlig43.tablecore.*`

Промежуточный результат проверки:
- `:features:storage`, `:features:sampletable`, `:features:analytic:profitability`, `:features:form:product`, `:features:form:transaction` компилируются
- первая проверка `:rootnocombro` выявила скрытую транзитивную зависимость в `:features:form:declaration`
- после этого `:features:form:declaration` был исправлен явной зависимостью на `table:core`
- повторная проверка `:rootnocombro:compileKotlinDesktop` прошла успешно
- первый шаг по `table:core` можно считать завершенным

Команды проверки, которые уже были выполнены:
- `./gradlew :features:storage:compileKotlinDesktop :features:sampletable:compileKotlinDesktop :features:analytic:profitability:compileKotlinDesktop :features:form:product:compileKotlinDesktop :features:form:transaction:compileKotlinDesktop`
- `./gradlew :rootnocombro:compileKotlinDesktop`

## Следующий логичный шаг

После успешной проверки сборки:

1. Найти модули, которые используют `ru.pavlig43.datetime.*` только транзитивно через `table:immutable` / `table:mutable`.
2. Сделать зависимости на `projects.datetime` явными у этих модулей.
3. После этого попробовать заменить `api(projects.datetime)` на `implementation(...)` в `table:immutable` и `table:mutable`.

## Следующие архитектурные шаги после Gradle cleanup

1. Ослабить `rootnocombro`:
   - сократить знание о concrete component constructors
   - двигаться к registry/factory-подходу для экранов
2. Развести capability-слои:
   - `files`
   - `manageitem:update`
   - `manageitem:loadinitdata`
3. Постепенно выдавливать прямые зависимости UI/features от `database`

## Файлы, которые уже затронуты

- `Aalekh-architecture-analysis.md`
- `ARCH_REFACTOR_PROGRESS.md`
- `features/table/immutable/build.gradle.kts`
- `features/table/mutable/build.gradle.kts`
- `rootnocombro/build.gradle.kts`

## Как продолжать в новой сессии

1. Открыть `ARCH_REFACTOR_PROGRESS.md`
2. Открыть `Aalekh-architecture-analysis.md`
3. Проверить сборку после шага с `table:core`
4. Продолжить с явным `datetime`

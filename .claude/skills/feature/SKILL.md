---
name: feature
description: Создание новой функциональности (таблицы, формы) в проекте Nocombro. Используй когда пользователь просит создать таблицу, форму или добавить новую feature.
---

# Создание Feature для $ARGUMENTS

## 1. Анализ сущности

Найди определение сущности:
```bash
find . -name "*Entity.kt" -o -name "*BD.kt" | grep -i "$ARGUMENTS"
```

Проверь поля:
- [ ] Поля с датой/временем? (`LocalDateTime`, `Instant`, `LocalDate`)
- [ ] Связи с другими сущностями? (`@Relation`)
- [ ] Это mutable или immutable таблица?

## 2. Выбор места выполнения (ОБЯЗАТЕЛЬНО!)

**ВСЕГДА** спрашивай перед началом работы. См. `.claude/rules/task-execution-scope.md`

```
Для этой задачи есть несколько вариантов:

1. **Git Worktree** — рекомендуется по умолчанию
2. **Новая ветка** — для features
3. **Текущая ветка** — только для typo fixes

Где выполнить задачу?
```

## 3. Применение правил

| Если есть... | Примени правило... |
|--------------|-------------------|
| Поля `LocalDateTime`, `Instant` | `.claude/rules/date-time-picker.md` |
| Запросы к Room Database | `.claude/rules/database.md` |
| Любая задача с кодом | `.claude/rules/task-execution-scope.md` |

Как применить правило:
```bash
cat .claude/rules/date-time-picker.md
```

## 4. Структура Feature

```
features/<feature-name>/
├── src/commonMain/kotlin/ru/pavlig43/<feature-name>/
│   ├── <Entity>Component.kt      # Decompose компонент
│   ├── <Entity>Screen.kt         # UI экран
│   ├── model/                    # Модели данных
│   └── columns/                  # Колонки таблицы
```

## 5. DI (Koin)

Добавь в `corekoin/.../module.kt`:
```kotlin
module {
    factory {
        MyComponent(
            componentContext = it.get(),
            // зависимости
        )
    }
}
```

## Чек-лист

- [ ] Проанализировал сущность
- [ ] Спросил пользователя где выполнить (worktree/ветка)
- [ ] Применил `.claude/rules/date-time-picker.md` (если есть дата/время)
- [ ] Применил `.claude/rules/database.md` (если есть БД)
- [ ] Создал Component/Screen
- [ ] Добавил в DI
- [ ] Проверил сборку `./gradlew build`

## Примеры в коде

| Что | Где |
|-----|-----|
| DateTime picker | `features/form/transaction/.../reminders/RemindersComponent.kt` |
| Пример таблицы | `features/sampletable/` |
| Decompose навигация | `rootnocombro/` |

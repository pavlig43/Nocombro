# Команда: /feature

**Назначение:** Создание новой функциональности (feature) в проекте Nocombro

## Когда использовать

Когда пользователь просит добавить новую функцию, создать таблицу, форму или другой компонент UI.

## Порядок выполнения

### 1. Анализ сущности

```bash
# Найти определение сущности
find . -name "*Entity.kt" -o -name "*BD.kt" | grep -i <название сущности>
```

**Проверь:**
- [ ] Есть ли поля с датой/временем? (`LocalDateTime`, `Instant`, `LocalDate`)
- [ ] Есть ли связи с другими сущностями? (`@Relation`)
- [ ] Это mutable или immutable таблица?

### 2. Выбор места выполнения (ОБЯЗАТЕЛЬНО!)

**ВСЕГДА** спрашивай перед началом работы. См. `.claude/rules/task-execution-scope.md`

```
Для этой задачи есть несколько вариантов:

1. **Git Worktree** — рекомендуется по умолчанию
2. **Новая ветка** — для features
3. **Текущая ветка** — только для typo fixes

Где выполнить задачу?
```

### 3. Применение правил

**После выбора места** примени соответствующие правила:

| Если есть... | Примени правило... |
|--------------|-------------------|
| Поля `LocalDateTime`, `Instant` | `.claude/rules/date-time-picker.md` |
| Запросы к Room Database | `.claude/rules/database.md` |
| Любая задача с кодом | `.claude/rules/task-execution-scope.md` |

**Как применить правило:**
```bash
# Прочитай правило
cat .claude/rules/date-time-picker.md

# И следуй инструкциям из правила
```

### 4. Структура Feature

```
features/<feature-name>/
├── src/commonMain/kotlin/ru/pavlig43/<feature-name>/
│   ├── <Entity>Component.kt      # Decompose компонент
│   ├── <Entity>Screen.kt         # UI экран
│   ├── model/                    # Модели данных
│   └── columns/                  # Колонки таблицы (если нужно)
```

### 5. DI (Koin)

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

---

**Важно:** Этот skill только направляет к правилам. Всю логику см. в `.claude/rules/*.md`

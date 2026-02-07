---
name: feature
description: Создание новой функциональности (таблицы, формы) в проекте Nocombro
---

# Создание Feature для: $ARGUMENTS

## 1. Анализ сущности

Найди определение сущности:
```bash
find . -name "*Entity.kt" -o -name "*BD.kt" | grep -i "$ARGUMENTS"
```

Проверь поля:
- Поля с датой/временем? (`LocalDateTime`, `Instant`, `LocalDate`)
- Связи с другими сущностями? (`@Relation`)
- Это mutable или immutable таблица?

## 2. Выбор места выполнения (ОБЯЗАТЕЛЬНО!)

**ВСЕГДА** спрашивай перед началом работу. См. `.claude/rules/task-execution-scope.md`

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

## 4. Структура Feature

```
features/<feature-name>/
├── <Entity>Component.kt
├── <Entity>Screen.kt
├── model/
└── columns/
```

## Чек-лист

- [ ] Проанализировал сущность
- [ ] Спросил пользователя где выполнить
- [ ] Применил `.claude/rules/date-time-picker.md` (если есть дата/время)
- [ ] Применил `.claude/rules/database.md` (если есть БД)
- [ ] Создал Component/Screen
- [ ] Добавил в DI
- [ ] Проверил сборку `./gradlew build`

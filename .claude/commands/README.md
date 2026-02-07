# Claude Commands для Nocombro

Пользовательские команды для автоматизации задач.

## Доступные команды

| Команда | Описание |
|---------|----------|
| `/feature <name>` | Создание новой функциональности |
| `/table <entity>` | Создание таблицы |
| `/form <entity>` | Создание формы |
| `/worktree <name>` | Создание git worktree |

## Использование

В Claude Code просто напиши:

```
/feature products
/table documents
/form vendors
/worktree add-auth
```

## Архитектура

Каждая команда:
1. Автоматически применяет правила из `.claude/rules/`
2. Проверяет чек-лист перед выполнением
3. Использует правильные паттерны проекта

## Связь с правилами

| Правило | Когда применяется |
|---------|-------------------|
| `date-time-picker.md` | Если есть `LocalDateTime` |
| `database.md` | Если есть запросы к БД |
| `task-execution-scope.md` | Всегда перед началом работы |

---
**Источник:** [Claude Code Docs - Skills](https://code.claude.com/docs/en/skills)

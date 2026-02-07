# Claude Skills для Nocombro

Эта директория содержит пользовательские skills (команды) для автоматизации задач в проекте.

## Доступные команды

| Команда | Описание |
|---------|----------|
| `/feature` | Создание новой функциональности (таблицы, формы) |
| `/table` | Создание таблицы (mutable/immutable) |
| `/form` | Создание формы для сущности |
| `/worktree` | Создание git worktree для параллельной работы |

## Как использовать

Просто напиши команду:

```
/feature products
/table documents
/form vendors
/worktree add-auth
```

## Архитектура

```
.claude/skills/
├── feature/SKILL.md    # Создание feature
├── table/SKILL.md      # Создание таблиц
├── form/SKILL.md       # Создание форм
└── worktree/SKILL.md   # Управление worktree
```

Каждый skill:
1. Автоматически применяет правила из `.claude/rules/`
2. Проверяет чек-лист перед выполнением
3. Использует правильные паттерны проекта

## Связь с правилами

| Правило | Когда применяется |
|---------|-------------------|
| `date-time-picker.md` | Если есть `LocalDateTime` в сущности |
| `database.md` | Если есть запросы к БД |
| `task-execution-scope.md` | Всегда перед началом работы |

---
**Источники:**
- https://code.claude.com/docs/en/skills
- https://fraway.io/blog/claude-code-skills-guide/

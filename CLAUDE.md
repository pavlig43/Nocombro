# CLAUDE.md

**Nocombro** — Kotlin Multiplatform приложение для управления складом и производством пищевой продукции.

---

## Быстрый старт

### Сборка
```bash
./gradlew build --continue     # Игнорируй [MissingType] от room-schema-docs
./gradlew :app:desktopApp:run  # Запуск Desktop
```

### Где выполнять задачи?
**ВСЕГДА** спрашивай перед началом работы. См. `.claude/rules/task-execution-scope.md`
1. **Git Worktree** (рекомендуется) — `git worktree add -b feature/имя ../путь`
2. **Новая ветка** — `git checkout -b feature/имя`
3. **Текущая ветка** — только для typo fixes

---

## Технологии

KMP • Compose Multiplatform • Decompose • Koin • Room Database • Kotlin Coroutines

---

## Структура проекта

```
root/
├── core/         # MainTabComponent, FormTabComponent
├── coreui/       # DateTimeRow, TextFieldRow
├── database/     # Room Entity, DAO
├── features/
│   ├── table/    # Immutable/Mutable таблицы
│   ├── form/     # Формы (product, declaration, vendor, etc.)
│   └── sign/     # Авторизация
└── app/
    ├── nocombroapp/    # Android
    └── desktopApp/     # Desktop
```

### Паттерны
- **MutableTableComponent** — таблицы с редактированием
- **ImmutableTableComponent** — read-only таблицы
- **UpdateSingleLineComponent** — одна строка редактирования
- **SlotNavigation** — управление диалогами

---

## Правила проекта (читай при необходимости)

| Правило | Когда нужно |
|---------|-------------|
| `.claude/rules/database.md` | Работа с БД — используй `@Relation` вместо JOIN |
| `.claude/rules/decimal-fields.md` | Поля с ценой/весом — храни в Int (копейки/граммы) |
| `.claude/rules/dialogs.md` | Диалоги — используй `SlotNavigation` для управления |
| `.claude/rules/date-time-picker.md` | Выбор даты/времени — DateTimeRow + SlotNavigation |
| `.claude/rules/imports.md` | Любой код — без wildcard импортов |
| `.claude/rules/agents.md` | Сложные задачи — делегируй экспертам |
| `.claude/rules/find-src.md` | Поиск API библиотек — используй `./.claude/tools/find-src` |

---

## Сущности БД

| Сущность | Описание |
|----------|----------|
| Vendor | Поставщик |
| Declaration | Декларация качества |
| Product | Товар/сырьё |
| Batch | Партия товара |
| BatchMovement | Движение партии |
| Transaction | BUY/SALE/OPZS/WRITE_OFF/INVENTORY |

Связи: `Product ↔ Declaration` (M:N), `Product → Batch` (1:N), `Batch → BatchMovement` (1:N)

Подробнее: `.claude/database-business-logic.md`

---

## Skills

| Skill | Описание |
|-------|----------|
| `/table` | Создать таблицу |
| `/form` | Создать форму |
| `/feature` | Создать функциональность |

---

## Troubleshooting

- **[MissingType] от room-schema-docs** — игнорируй, используй `--continue`
- **Не могу найти API** — `./.claude/tools/find-src --content "ClassName"`

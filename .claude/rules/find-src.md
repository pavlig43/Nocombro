# Правило: Используй find-src для поиска в зависимостях

## Когда использовать

**ВСЕГДА** используй `./.claude/tools/find-src` для поиска исходного кода Kotlin библиотек, вместо того чтобы:
- Копаться в `.gradle/caches`
- Искать вручную в интернете
- Просить пользователя найти API

## Команды для использования

**1. Поиск класса/функции в зависимостях:**
```bash
./.claude/tools/find-src --content "ClassName" [--artifact artifactId]
```

**2. Чтение файла по найденному пути:**
```bash
./.claude/tools/find-src cat <jar-path> <file-path>
```

**3. Поиск по имени файла:**
```bash
./.claude/tools/find-src <filename> [artifact]
```

**4. Список всех файлов в артефакте:**
```bash
./.claude/tools/find-src list <artifact>
```

## Примеры использования

### Decompose
```bash
# Поиск SlotNavigation
./.claude/tools/find-src --content "SlotNavigation" decompose

# Чтение интерфейса
./.claude/tools/find-src cat <jar-path> "commonMain/.../SlotNavigation.kt"
```

### Room
```bash
# Поиск @Relation
./.claude/tools/find-src --content "@Relation" room-runtime
```

### Любой класс из библиотек
```bash
# Просто скажи имя класса
./.claude/tools/find-src --content "ActorCoroutine" coroutines
```

## ВАЖНО

- **Всегда** начинай с `./.claude/tools/find-src --content` когда нужно понять API библиотеки
- **Не спрашивай пользователя** "где найти" — используй find-src
- **Не смотри в .gradle** — find-src сделает это быстрее
- Указывай `--artifact` для ускорения поиска (опционально)
- `--content` ищет по содержимому файлов (классы, функции)
- Без `--content` ищет только по именам файлов

## Расположение

Инструмент находится в `.claude/tools/find-src` и всегда доступен в проекте.

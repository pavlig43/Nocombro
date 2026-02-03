# Nocombro - Kotlin Multiplatform Application

## Project Overview

Nocombro — это Kotlin Multiplatform приложение для управления документами, товарами, транзакциями и подписями. Приложение работает на **Android** и **Desktop** платформах.

## Tech Stack

| Технология | Версия | Для чего |
|------------|--------|----------|
| **Kotlin Multiplatform** | - | Общая кодовая база для Android + Desktop |
| **Compose Multiplatform** | - | UI фреймворк |
| **Koin** | - | Dependency Injection |
| **Decompose** | - | Навигация и управление компонентами |
| **SQLDelight** | - | Локальная база данных |
| **Room** | - | Альтернативная БД (используется параллельно) |
| **Detekt** | - | Статический анализ кода |
| **Gradle** | - | Система сборки с custom convention plugins |

## Project Structure

```
Nocombro/
├── app/                          # Приложения
│   ├── nocombroapp/              # Android приложение
│   └── desktopApp/               # Desktop приложение
│
├── core/                         # Core бизнес-логика
├── coreui/                       # Общие UI компоненты
├── corekoin/                     # Koin DI модули
├── database/                     # База данных
├── datastore/                    # DataStore для настроек
├── theme/                        # Темы оформления
│
├── features/                     # Feature модули
│   ├── sign/                     # Авторизация
│   │   ├── common/               # Общая логика sign
│   │   ├── signin/               # Вход
│   │   ├── signup/               # Регистрация
│   │   └── root/                 # Root sign компонент
│   │
│   ├── table/                    # Таблицы
│   │   ├── core/                 # Core логика таблиц
│   │   ├── immutable/            # Immutable таблицы
│   │   └── mutable/              # Mutable таблицы
│   │
│   ├── form/                     # Формы
│   │   ├── product/              # Товары
│   │   ├── vendor/               # Поставщики
│   │   ├── document/             # Документы
│   │   ├── declaration/          # Декларации
│   │   └── transaction/          # Транзакции
│   │
│   ├── manageitem/               # Управление элементами
│   │   ├── upsert/               # Создание/обновление
│   │   └── loadinitdata/         # Загрузка начальных данных
│   │
│   ├── storage/                  # Хранилище
│   ├── files/                    # Файлы
│   ├── notification/             # Уведомления
│   └── sampletable/              # Пример таблицы
│
├── rootnocombro/                 # Root компонент приложения
├── build-logic/                  # Custom Gradle plugins
│   └── convention/               # Convention plugins для KMP
│
├── build.gradle.kts              # Root build file
├── settings.gradle.kts           # Gradle settings
└── default-detekt-config.yml     # Detekt конфигурация
```

## Module Architecture

### Core Modules
- **core** — общая бизнес-логика, не зависящая от платформы
- **coreui** — переиспользуемые UI компоненты
- **corekoin** — Koin модули для DI
- **database** — слой доступа к данным (SQLDelight/Room)
- **datastore** — хранение настроек (DataStore)
- **theme** — дизайн-система (цвета, типографика)

### Feature Modules
Каждый feature модуль — это автономная часть с UI, логикой и DI.

### Platform-Specific
- **commonMain** — общий код для всех платформ
- **androidMain** — Android-specific код
- **desktopMain** — Desktop-specific код

## Как запускать

### Android
```bash
./gradlew :app:nocombroapp:installDebug
```

### Desktop
```bash
./gradlew :app:desktopApp:run
```

### Сборка всех модулей
```bash
./gradlew build
```

## Тестирование

### Запустить тесты
```bash
# Все тесты
./gradlew test

# Тесты конкретного модуля
./gradlew :core:test
```

### Статический анализ (Detekt)
```bash
./gradlew detekt
```

## Gradle Convention Plugins

Проект использует custom convention plugins из `build-logic/convention/`:

| Plugin | Для чего |
|--------|----------|
| `pavlig43.kmplibrary` | Базовая конфигурация KMP библиотеки |
| `pavlig43.serialization` | Kotlinx Serialization |
| `pavlig43.coroutines` | Coroutines |
| `pavlig43.koin` | Koin DI |
| `pavlig43.ktor` | Ktor HTTP клиент |
| `pavlig43.feature` | Feature модули |
| `pavlig43.sqldelight` | SQLDelight |
| `pavlig43.decompose` | Decompose навигация |
| `pavlig43.detekt` | Detekt статический анализ |

## Создание нового feature модуля

```bash
./gradlew createKmpLib -PmoduleName=myfeature
```

Эта команда:
1. Создаст структуру директорий в `features/myfeature/`
2. Создаст `build.gradle.kts` с плагином `pavlig43.feature`
3. Добавит модуль в `settings.gradle.kts`

## Git Workflow

### Ветки
- `separate_application_in_module` — текущая ветка (разделение ApplicationPlugin)
- `main` / `master` — основная ветка

### Коммиты
Используется `Co-Authored-By:` для указания моего участия:

```
Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

## Code Style

- **Detekt** — статический анализ (см. `default-detekt-config.yml`)
- **Kotlin conventions** — следуй Kotlin coding conventions
- **KDoc** — добавляй документацию к публичным API

## Claude Code Subagents

В проекте установлены следующие агенты (`.claude/agents/`):

| Агент | Специализация |
|-------|---------------|
| `kotlin-specialist` | Kotlin/KMP разработка |
| `java-architect` | Java архитектура |
| `mobile-developer` | Мобильная разработка |
| `code-reviewer` | Ревью кода |
| `debugger` | Отладка |
| `security-auditor` | Безопасность |

### Использование агентов

```
> Используй kotlin-specialist для рефакторинга этого модуля
> Пусть code-reviewer проверит мой коммит
```

## Конфигурация Claude Code

### Project settings (`.claude/settings.local.json`)
Разрешённые bash команды для git, gradle, gh (GitHub CLI) и других операций.

### Global settings (`~/.claude/settings.json`)
- Язык: русский
- Модель: Sonnet 4.5
- Плагины: voltagent-*

## Полезные команды

### Очистка
```bash
./gradlew clean
```

### Зависимости
```bash
./gradlew :app:nocombroapp:dependencies
```

### Dry-run (без реального выполнения)
```bash
./gradlew build --dry-run
```

## FAQ

**Q: Где находится DI конфигурация?**
A: В модулях `corekoin` и `build.gradle.kts` каждого модуля.

**Q: Как добавить новый экран?**
A: Создай feature модуль, используй Decompose для навигации.

**Q: Где бизнес-логика?**
A: В `core/` и соответствующих feature модулях.

**Q: Как работать с БД?**
A: Используй `database/` модуль, SQLDelight `.sq` файлы для запросов.

---

**Важно:** Этот файл (`CLAUDE.md`) предназначен для Claude Code AI и содержит контекст проекта для более эффективной работы.

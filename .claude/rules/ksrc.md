# Правило: Используй ksrc для поиска в зависимостях

## Когда использовать

**Всегда** используй `ksrc` для поиска исходного кода Kotlin библиотек, вместо того чтобы:
- Копаться в `.gradle/caches`
- Искать вручную в интернете
- Просить пользователя найти API

## Команды для использования

**1. Поиск функции/класса в зависимостях:**
```bash
export PATH="$(pwd)/tools:$PATH"
./tools/ksrc.exe search "название" [--artifact artifactId]
```

**2. Чтение файла по найденному пути:**
```bash
./tools/ksrc.exe cat 'group:artifact:version!/path/to/File.kt'
```

**3. Список зависимостей:**
```bash
./tools/ksrc.exe deps
```

## Примеры использования

### Decompose
```bash
# Поиск SlotNavigation
./tools/ksrc.exe search "SlotNavigation" --artifact decompose

# Чтение интерфейса
./tools/ksrc.exe cat 'com.arkivanov.decompose:decompose:3.4.0!/commonMain/com/arkivanov/decompose/router/slot/SlotNavigation.kt'
```

### Koin
```bash
# Поиск функции inject или module
./tools/ksrc.exe search "inject" --artifact koin-core

# Поиск KoinApplication
./tools/ksrc.exe search "KoinApplication" --artifact koin-core
```

### Room
```bash
# Поиск @Relation или @Query
./tools/ksrc.exe search "@Relation" --artifact room-runtime
```

## ВАЖНО

- **Всегда** начинай с `ksrc search` когда нужно понять API библиотеки
- **Не спрашивай пользователя** "где найти" — используй ksrc
- **Не смотри в .gradle** — ksrc сделает это быстрее
- export PATH="$(pwd)/tools:$PATH" — обязательно для работы

## Установленные инструменты

- `tools/ksrc.exe` (v0.6.0)
- `tools/rg.exe` (ripgrep 15.1.0)

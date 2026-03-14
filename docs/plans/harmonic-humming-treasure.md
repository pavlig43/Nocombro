# План: Коммит изменений expense form

## Обзор

Закоммитить все изменения (staged + unstaged), связанные с добавлением expense form и её интеграцией в приложение.

## Изменения

### Staged (14 файлов, +494 строки)
- Новая expense форма (build.gradle.kts, Components, Tabs, Columns, UI)
- DI модуль (ExpenseFormModule)

### Unstaged (33 файла, +102 -97)
- Интеграция в MainTabNavigation (TabOpener, MainTabChild, MainTabConfig)
- DI в RootNocombroModule
- Обновление settings.gradle.kts
- Рефакторинг (replace onCreate with tabOpener в компонентах)

## Действия

### 1. Добавить все файлы в staging
```bash
git add .
```

### 2. Создать коммит
```bash
git commit -m "$(cat <<'EOF'
feat: Add expense form with navigation integration

- Add expense form module with table component
- Integrate expense form into main navigation
- Add expense tab to MainTabNavigation
- Update DI modules (RootNocombroModule, ExpenseFormModule)
- Refactor: Replace onCreate with tabOpener in components
- Update settings.gradle.kts to include expense module

Co-Authored-By: Claude (glm-4.7) <noreply@anthropic.com>
EOF
)"
```

### 3. Проверить результат
```bash
git status
git log --oneline -1
```

## Проверка

После коммита проверить:
- `git status` показывает чистое состояние (no changes to commit)
- Последний коммит в `git log` содержит правильное сообщение

## Критические файлы

- `features/form/expense/` - новая функциональность
- `rootnocombro/.../navigation/` - интеграция навигации
- `rootnocombro/.../di/RootNocombroModule.kt` - DI регистрация
- `settings.gradle.kts` - включение модуля

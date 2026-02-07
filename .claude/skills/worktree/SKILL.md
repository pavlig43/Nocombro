---
name: worktree
description: Создаёт новый git worktree для параллельной работы над задачами. Используй когда нужно работать над несколькими задачами одновременно или сохранить текущее состояние нетронутым.
disable-model-invocation: true
---

# Создание Git Worktree для $ARGUMENTS

Эта команда создаёт новый git worktree для изолированной работы над задачей.

## Использование

/worktree <branch-name>

## Что делает

1. Создаёт новую ветку с именем `feature/<branch-name>` (или использует существующую)
2. Создаёт новый worktree в директории `../Nocombro-<branch-name>`
3. Переключает контекст работы в новый worktree

## Команды

```bash
# Создание worktree с новой веткой
git worktree add -b feature/$ARGUMENTS ../Nocombro-$ARGUMENTS

# Или для существующей ветки
git worktree add ../Nocombro-$ARGUMENTS feature/$ARGUMENTS
```

## После создания

1. Переключись в новый worktree: `cd ../Nocombro-$ARGUMENTS`
2. Проверь статус: `git status`
3. Работай в изолированном окружении

## Управление worktree

```bash
# Список всех worktree
git worktree list

# Удаление worktree после завершения работы
git worktree remove ../Nocombro-$ARGUMENTS

# Очистка удалённых веток
git worktree prune
```

## Преимущества

- Изолированная работа над несколькими задачами
- Текущая работа остаётся нетронутой
- Быстрое переключение между задачами через `cd`

## Пример

/worktree add-user-auth
→ Создаст `../Nocombro-add-user-auth` с веткой `feature/add-user-auth`

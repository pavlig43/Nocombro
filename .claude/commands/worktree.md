---
name: worktree
description: Создаёт новый git worktree для параллельной работы
disable-model-invocation: true
---

# Создание Git Worktree для: $ARGUMENTS

## Создаёт новый worktree

```bash
git worktree add -b feature/$ARGUMENTS ../Nocombro-$ARGUMENTS
```

## После создания

1. `cd ../Nocombro-$ARGUMENTS`
2. `git status`
3. Работай в изолированном окружении

## Управление

```bash
# Список всех worktree
git worktree list

# Удаление после завершения
git worktree remove ../Nocombro-$ARGUMENTS

# Очистка
git worktree prune
```

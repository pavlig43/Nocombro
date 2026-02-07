#!/bin/bash

# Status line script для Claude Code
# Показывает полезную информацию о текущем состоянии

# Текущая ветка git
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "no-git")

# Количество модифицированных файлов
MODIFIED=$(git status --short 2>/dev/null | wc -l | tr -d ' ')

# Текущая директория
DIR=$(basename "$PWD")

# Вывод статуса
if [ "$MODIFIED" -gt 0 ]; then
    echo "📁 $DIR | 🌿 $BRANCH | 📝 $MODIFIED files"
else
    echo "📁 $DIR | 🌿 $BRANCH"
fi

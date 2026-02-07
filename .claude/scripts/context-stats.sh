#!/bin/bash

# Скрипт для отображения статистики контекста и токенов Claude Code

# Проверяем есть ли переменная окружения с статистикой
if [ -n "$CLAUDE_CONTEXT_TOKENS" ]; then
    USED=$CLAUDE_CONTEXT_TOKENS
    MAX=${CLAUDE_MAX_TOKENS:-200000}
    PERCENT=$((USED * 100 / MAX))
    
    # Форматируем вывод
    if [ $PERCENT -lt 50 ]; then
        COLOR="🟢"
    elif [ $PERCENT -lt 80 ]; then
        COLOR="🟡"
    else
        COLOR="🔴"
    fi
    
    echo "📊 $USED/$MAX tokens ($PERCENT%) $COLOR"
else
    echo "📊 Context: N/A"
fi

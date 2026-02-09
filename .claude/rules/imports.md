# Правило: Импорты в Kotlin

## ❌ ЗАПРЕЩЕНО: Wildcard импорты

**НИКОГДА** не используй wildcard импорты вида `import androidx.compose.material3.*`

### Плохо
```kotlin
import androidx.compose.material3.*
import ru.pavlig43.mutable.api.*
```

### Хорошо
```kotlin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import ru.pavlig43.mutable.api.column.writeTextColumn
```

## Почему

- **Явность** — сразу видно какие компоненты используются
- **IDE поддержка** — лучше работает автокомплит и навигация
- **Конфликты** — избегаются проблемы с одинаковыми именами
- **Чистота** — не импортируется лишнее

## Исключения

Нет исключений. Всегда используй конкретные импорты.

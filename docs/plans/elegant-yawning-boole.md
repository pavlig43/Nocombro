# План: Унификация через Regex

### 1. `DecimalData.kt` - добавить extension
```kotlin
val DecimalData.inputRegex: Regex
    get() = Regex("^\\d*(\\.\\d{0,$countDecimal})?$")
```

### 2. `DecimalColumn.kt:151-174` - заменить на fold с regex

**Было:** fold + when + split + take
**Станет:** fold + regex (проверяет candidate на каждом шаге, regex сам ограничивает дробную часть)

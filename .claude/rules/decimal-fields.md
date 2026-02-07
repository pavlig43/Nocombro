# Правила: Работа с десятичными числами (кг, рубли)

## Принцип хранения

**ПРАВИЛО:** Для хранения денежных единиц (рубли) и весовых категорий (кг) **ВСЕГДА** используй `Int` вместо `Double` или `BigDecimal`.

### Почему Int?

- **Избегает ошибок округления** — `0.1 + 0.2 != 0.3` в Float/Double
- **Точность** — целые числа не теряют точность при операциях
- **Производительность** — Int быстрее BigDecimal
- **Простота** — легче сериализовать и сравнивать

### Хранение в малых единицах

| Что отображается | Что хранится в БД | Множитель | Пример |
|-----------------|-------------------|----------|--------|
| **Рубли** | **Копейки** | ×100 | 150.50 ₽ → 15050 (копеек) |
| **Килограммы** | **Граммы** | ×1000 | 2.500 кг → 2500 (граммов) |

---

## Использование в коде

### 1. DecimalFormat

Используй готовый sealed interface из `TableCellTextFieldNumber.kt:56-67`:

```kotlin
import ru.pavlig43.mutable.api.ui.DecimalFormat

sealed interface DecimalFormat {
    val countDecimal: Int
    
    class KG : DecimalFormat {
        override val countDecimal: Int = 3  // 3 знака для кг
    }
    
    class RUB : DecimalFormat {
        override val countDecimal: Int = 2  // 2 знака для рублей
    }
}
```

### 2. Создание колонки таблицы

Используй функцию `decimalColumn` из `TableCellTextFieldNumber.kt:22-54`:

```kotlin
import ru.pavlig43.mutable.api.ui.decimalColumn
import ru.pavlig43.mutable.api.ui.DecimalFormat

// Для веса (кг)
decimalColumn(
    key = Columns.Weight,
    getValue = { it.weightInGrams },  // Int: граммы
    headerText = "Вес (кг)",
    decimalFormat = DecimalFormat.KG(),
    onEvent = ::onEvent,
    updateItem = { item, newGrams -> item.copy(weightInGrams = newGrams) },
    footerValue = { items -> items.sumOf { it.weightInGrams } }  // опционально
)

// Для денег (рубли)
decimalColumn(
    key = Columns.Price,
    getValue = { it.priceInKopecks },  // Int: копейки
    headerText = "Цена (₽)",
    decimalFormat = DecimalFormat.RUB(),
    onEvent = ::onEvent,
    updateItem = { item, newKopecks -> item.copy(priceInKopecks = newKopecks) },
    footerValue = { items -> items.sumOf { it.priceInKopecks } }
)
```

### 3. Entity/БД

```kotlin
@Entity
data class ProductEntity(
    val id: Int,
    val name: String,
    val weightInGrams: Int,      // Храним граммы, не кг!
    val priceInKopecks: Int      // Храним копейки, не рубли!
)

// Использование
val product = ProductEntity(
    id = 1,
    name = "Товар",
    weightInGrams = 2500,  // 2.5 кг
    priceInKopecks = 15050 // 150.50 ₽
)
```

### 4. Конвертация при отображении

**НЕ НУЖНО** — функция `decimalColumn` делает это автоматически!

Если нужно вручную:

```kotlin
// Int → String для отображения
fun Int.gramsToKg(): String = 
    (this / 1000.0).toString().dropLastWhile { it == '0' }
        .run { if (last() == '.') dropLast(1) else this }

fun Int.kopecksToRubles(): String = 
    (this / 100.0).toString().dropLastWhile { it == '0' }
        .run { if (last() == '.') dropLast(1) else this }

// String → Int для сохранения
fun String.kgToGrams(): Int = 
    (toDouble() * 1000).toInt()

fun String.rublesToKopecks(): Int = 
    (toDouble() * 100).toInt()
```

---

## Когда применять правило

**ПРОВЕРЬ Entity:**

```bash
# Если есть поля с такими именами - используй это правило:
find . -name "*Entity.kt" -exec grep -l "weight\|price\|cost\|amount" {} \;
```

| Имя поля | Что хранить | DecimalFormat |
|----------|-------------|---------------|
| `weightInGrams`, `mass` | Граммы | `DecimalFormat.KG()` |
| `priceInKopecks`, `cost`, `amount` | Копейки | `DecimalFormat.RUB()` |

---

## ❌ НЕ ДЕЛАЙ ТАК

```kotlin
// ПЛОХО - используешь Double/BigDecimal
data class Product(
    val weight: Double,      // ❌ Нет!
    val price: BigDecimal   // ❌ Нет!
)

// ПЛОХО - хранишь рубли/кг напрямую
data class Product(
    val weightKg: Int,      // ❌ 2.5 кг не хранится!
    val priceRubles: Int    // ❌ 150.50 ₽ не хранится!
)
```

---

## ✅ ДЕЛАЙ ТАК

```kotlin
// ХОРОШО - используешь Int для хранения
data class Product(
    val weightInGrams: Int,     // ✅ 2500 граммов
    val priceInKopecks: Int     // ✅ 15050 копеек
)

// ХОРОШО - используешь decimalColumn для отображения
decimalColumn(
    key = Columns.Weight,
    getValue = { it.weightInGrams },
    decimalFormat = DecimalFormat.KG(),
    // ...
)
```

---

## Дополнительные возможности

### Footer (итоги в колонке)

```kotlin
decimalColumn(
    // ...
    footerValue = { tableData ->
        tableData.items.sumOf { it.weightInGrams }
    }
)
```

Это покажет сумму весов/цен в футере таблицы.

### Фильтрация

`decimalColumn` автоматически добавляет числовой фильтр для колонки.

---

## Связанные правила

- `.claude/rules/database.md` — для работы с БД
- `.claude/rules/date-time-picker.md` — для полей с датой/временем

---

## Источник

Файл: `features/table/mutable/src/commonMain/kotlin/ru/pavlig43/mutable/api/ui/TableCellTextFieldNumber.kt`

Функции:
- `decimalColumn()` — строка 22
- `DecimalFormat` — строка 56
- `toStartDoubleFormat()` — строка 92

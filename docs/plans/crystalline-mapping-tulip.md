# План: Decimal Filter Delegates

## Задача

Создать два делегата для фильтрации decimal полей:
- `Decimal2Delegate` - для рублей (2 знака)
- `Decimal3Delegate` - для кг (3 знака)

---

## Реализация

**Файл:** `core/src/desktopMain/kotlin/ru/pavlig43/core/model/DecimalDataDelegate.kt` (новый)

```kotlin
import ua.wwind.table.filter.data.TableFilterType

object Decimal2Delegate : TableFilterType.NumberTableFilter.NumberFilterDelegate<DecimalData> {
    override val regex = Regex("^-?\\d*(\\.\\d{0,2})?$")
    override val default = DecimalData(0, DecimalFormat.Decimal2)

    override fun parse(input: String): DecimalData? {
        val doubleValue = input.toDoubleOrNull() ?: return null
        val intValue = (doubleValue * 100).toInt()
        return DecimalData(intValue, DecimalFormat.Decimal2)
    }

    override fun format(value: DecimalData): String = value.toStartDoubleFormat()
    override fun toSliderValue(value: DecimalData): Float = (value.value / 100.0).toFloat()
    override fun fromSliderValue(value: Float): DecimalData =
        DecimalData((value * 100).toInt(), DecimalFormat.Decimal2)
    override fun compare(a: DecimalData, b: DecimalData): Boolean = a.value <= b.value
}

object Decimal3Delegate : TableFilterType.NumberTableFilter.NumberFilterDelegate<DecimalData> {
    override val regex = Regex("^-?\\d*(\\.\\d{0,3})?$")
    override val default = DecimalData(0, DecimalFormat.Decimal3)

    override fun parse(input: String): DecimalData? {
        val doubleValue = input.toDoubleOrNull() ?: return null
        val intValue = (doubleValue * 1000).toInt()
        return DecimalData(intValue, DecimalFormat.Decimal3)
    }

    override fun format(value: DecimalData): String = value.toStartDoubleFormat()
    override fun toSliderValue(value: DecimalData): Float = (value.value / 1000.0).toFloat()
    override fun fromSliderValue(value: Float): DecimalData =
        DecimalData((value * 1000).toInt(), DecimalFormat.Decimal3)
    override fun compare(a: DecimalData, b: DecimalData): Boolean = a.value <= b.value
}
```

---

## Файл

- **Создать:** `core/src/desktopMain/kotlin/ru/pavlig43/core/model/DecimalDataDelegate.kt`

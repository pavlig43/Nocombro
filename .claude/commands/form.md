---
name: form
description: Создание формы для создания/редактирования сущности
---

# Создание формы для: $ARGUMENTS

## 1. Анализ полей

```bash
find . -name "*Entity.kt" | grep -i "$ARGUMENTS"
```

Поля:
- Текст → `TextFieldRow`
- Целые числа → `IntFieldRow`
- Дата/время → см. `.claude/rules/date-time-picker.md`
- **Деньги (рубли)** → см. `.claude/rules/decimal-fields.md`
- **Вес (кг)** → см. `.claude/rules/decimal-fields.md`
- Список → `DropdownRow`

## 2. Компоненты из `coreui`

Путь: `coreui/src/commonMain/kotlin/ru/pavlig43/coreui/coreFieldBlock/`

| Компонент | Для чего |
|-----------|----------|
| TextFieldRow | Текст |
| IntFieldRow | Целые числа |
| BigDecimalFieldRow | Десятичные числа |
| DateTimeRow | Дата + время |
| DateRow | Только дата |
| DropdownRow | Выбор из списка |

### Для денег и веса

Используй `decimalColumn` для таблиц. Для форм используй стандартные компоненты с конвертацией:

```kotlin
// В Entity храним Int (копейки/граммы)
data class Product(
    val priceInKopecks: Int,    // Храним копейки
    val weightInGrams: Int      // Храним граммы
)

// В форме конвертируем при отображении/сохранении
@Composable
fun ProductFormScreen(component: ProductFormComponent) {
    val priceRubles by derivedStateOf {
        component.formData.priceInKopecks / 100.0
    }
    
    BigDecimalFieldRow(
        label = "Цена (₽)",
        value = priceRubles.toBigDecimal(),
        onValueChange = { newRubles ->
            component.onEvent(
                FormEvent.UpdatePrice(
                    (newRubles * 100.toBigDecimal()).toInt()
                )
            )
        }
    )
}
```

## 3. Структура

```
features/<feature>/
├── <Entity>FormComponent.kt
├── <Entity>FormScreen.kt
└── model/<Entity>FormData.kt
```

## 4. FormComponent

```kotlin
internal class MyFormComponent(
    componentContext: ComponentContext,
    private val onSave: (MyEntity) -> Unit
) : ComponentContext by componentContext {

    private val formData = mutableStateOf(MyEntity())
    private val dialogNavigation = SlotNavigation<FormDialog>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "form_dialog",
        serializer = FormDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    fun onEvent(event: FormEvent) {
        when (event) {
            is FormEvent.Save -> onSave(formData.value)
            is FormEvent.UpdatePrice -> {
                formData.value = formData.value.copy(priceInKopecks = event.kopecks)
            }
            is FormEvent.UpdateWeight -> {
                formData.value = formData.value.copy(weightInGrams = event.grams)
            }
            is FormEvent.OpenDatePicker -> {
                dialogNavigation.activate(FormDialog.DatePicker)
            }
        }
    }
}
```

## 5. FormScreen

```kotlin
@Composable
internal fun MyFormScreen(component: MyFormComponent) {
    val dialog by component.dialog.subscribeAsState()
    val data by component.formData

    Column {
        // Текст
        TextFieldRow(
            label = "Название",
            value = data.name,
            onValueChange = { component.onEvent(FormEvent.UpdateName(it)) }
        )
        
        // Цена (рубли)
        // Конвертируем копейки → рубли для отображения
        val priceRubles by remember {
            derivedStateOf { data.priceInKopecks / 100.0 }
        }
        BigDecimalFieldRow(
            label = "Цена (₽)",
            value = priceRubles.toBigDecimal(),
            onValueChange = { newRubles ->
                // Конвертируем рубли → копейки для сохранения
                component.onEvent(
                    FormEvent.UpdatePrice((newRubles * 100).toInt())
                )
            }
        )
        
        // Вес (кг)
        val weightKg by remember {
            derivedStateOf { data.weightInGrams / 1000.0 }
        }
        BigDecimalFieldRow(
            label = "Вес (кг)",
            value = weightKg.toBigDecimal(),
            onValueChange = { newKg ->
                component.onEvent(
                    FormEvent.UpdateWeight((newKg * 1000).toInt())
                )
            }
        )
        
        // Дата
        DateTimeRow(
            date = data.createdAt,
            isChangeDialogVisible = {
                component.onEvent(FormEvent.OpenDatePicker)
            }
        )
    }

    dialog.child?.instance?.also { /* ... */ }
}
```

## Чек-лист

- [ ] Проанализировал поля
- [ ] Проверил date-time-picker.md
- [ ] Проверил decimal-fields.md для денег/веса
- [ ] Спросил где выполнить
- [ ] Выбрал компоненты из coreui
- [ ] Добавил конвертацию для денег/веса
- [ ] Создал FormComponent
- [ ] Создал FormScreen
- [ ] Добавил в DI

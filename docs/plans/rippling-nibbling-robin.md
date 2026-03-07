# Исправление проблемы с Code Inspection

## Проблема

Code Inspection в Android Studio зависает при анализе `FormTabsUi.kt`.

**Истинная причина:** Топ-левел `val` свойства с `@Composable` лямбдами (строки 74-88):

```kotlin
val tabBorder = @Composable { isSelected: Boolean -> ... }
val tabOnClickModifier = @Composable { ... -> ... }
```

Android Studio Code Inspection не expects `@Composable` на свойствах верхнего уровня, что вызывает зависание.

## Решение

Переместить эти `val` свойства внутрь `@Composable` функции или преобразовать в обычные функции.

## Шаги выполнения

### Файл для изменения

```
features/manageitem/update/src/commonMain/kotlin/ru/pavlig43/update/ui/FormTabsUi.kt
```

### Изменения

**Заменить строки 68-88:**

```kotlin
// УДАЛИТЬ эти топ-левел val:
val tabSize = DpSize(228.dp, 36.dp)
val tabModifier = Modifier.size(tabSize).padding(8.dp)
val tabBorder = @Composable { isSelected: Boolean -> ... }
val tabOnClickModifier = @Composable { ... -> ... }
```

**На код внутри TabContent функции:**

```kotlin
@Composable
private fun TabContent(
    formTabComponent: FormTabComponent,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabSize = DpSize(228.dp, 36.dp)
    val tabModifier = Modifier.size(tabSize).padding(8.dp)

    val interactionSource = remember(formTabComponent) { MutableInteractionSource() }
    val pressedAsState = interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressedAsState.value) {
        if (pressedAsState.value) {
            onSelect()
        }
    }

    @Composable
    fun tabBorder(isSelected: Boolean): BorderStroke {
        return if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
        } else {
            CardDefaults.outlinedCardBorder()
        }
    }

    @Composable
    fun tabOnClickModifier(onSelect: () -> Unit, interactionSource: MutableInteractionSource): Modifier {
        return Modifier.clickable(
            onClick = onSelect,
            interactionSource = interactionSource,
            indication = null
        )
    }

    OutlinedCard(
        modifier = modifier.then(tabOnClickModifier(onSelect, interactionSource)),
        colors = CardDefaults.outlinedCardColors().copy(),
        border = tabBorder(isSelected)
    ) {
        Row(
            modifier = tabModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formTabComponent.title,
                textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None
            )
        }
    }
}
```

## Проверка

После изменения:
1. Запусти **Code** → **Inspect Code**
2. Убедись, что инспекция проходит без зависаний
3. Проверь, что UI отображается корректно

## Примечание

Проблема только в статическом анализе Android Studio. На работу приложения это не влияет.

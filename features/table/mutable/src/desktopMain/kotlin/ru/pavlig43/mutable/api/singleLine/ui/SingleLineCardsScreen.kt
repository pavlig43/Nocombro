package ru.pavlig43.mutable.api.singleLine.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponent
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

/** Поле карточной формы с подписью и UI-содержимым. */
data class SingleLineFormField<I : Any>(
    val label: String,
    val content: @Composable (item: I, modifier: Modifier) -> Unit,
)

/** Явно заданная строка полей внутри карточки. */
data class SingleLineFormRow<I : Any>(
    val fields: ImmutableList<SingleLineFormField<I>>,
)

/**
 * Секция формы, показанная отдельной карточкой.
 *
 * @param title заголовок карточки
 * @param rows явно заданные строки полей
 * @param actions необязательные действия справа от заголовка
 */
data class SingleLineFormSection<I : Any>(
    val title: String,
    val rows: ImmutableList<SingleLineFormRow<I>>,
    val actions: (@Composable (item: I) -> Unit)? = null,
)

/** Создаёт строку из одного или нескольких полей одинаковой ширины. */
fun <I : Any> singleLineFormRow(
    vararg fields: SingleLineFormField<I>,
): SingleLineFormRow<I> = SingleLineFormRow(persistentListOf(*fields))

/**
 * Показывает single-форму как список карточек.
 *
 * @param component источник модели и событий формы
 * @param sections готовая схема секций и строк
 */
@Composable
fun <I : ISingleLineTableUi> SingleLineCardsScreen(
    component: SingleLineComponent<*, I, *>,
    sections: ImmutableList<SingleLineFormSection<I>>,
    modifier: Modifier = Modifier,
) {
    LoadInitDataScreen(component.initDataComponent) {
        val item by component.item.collectAsState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            sections.forEach { section ->
                SingleLineSectionCard(
                    section = section,
                    item = item,
                )
            }
        }
    }
}

/** Показывает одну секцию формы как карточку. */
@Composable
private fun <I : Any> SingleLineSectionCard(
    section: SingleLineFormSection<I>,
    item: I,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = section.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                section.actions?.invoke(item)
            }

            section.rows.forEach { row ->
                SingleLineFormRowContent(
                    row = row,
                    item = item,
                )
            }
        }
    }
}

/** Показывает одну явно заданную строку полей. */
@Composable
private fun <I : Any> SingleLineFormRowContent(
    row: SingleLineFormRow<I>,
    item: I,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        row.fields.forEach { field ->
            SingleLineFormFieldContent(
                field = field,
                item = item,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Показывает подпись и содержимое одного поля. */
@Composable
private fun <I : Any> SingleLineFormFieldContent(
    field: SingleLineFormField<I>,
    item: I,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = field.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        field.content(item, Modifier.fillMaxWidth())
    }
}

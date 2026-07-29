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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponent
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

data class SingleLineFormField<I : Any>(
    val label: String,
    val fullWidth: Boolean = false,
    val content: @Composable (item: I, modifier: Modifier) -> Unit,
)

data class SingleLineFormSection<I : Any>(
    val title: String,
    val fields: ImmutableList<SingleLineFormField<I>>,
)

@Composable
fun <I : ISingleLineTableUi, C> SingleLineCardsScreen(
    component: SingleLineComponent<*, I, C>,
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
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            var fieldIndex = 0
            while (fieldIndex < section.fields.size) {
                val firstField = section.fields[fieldIndex]
                val secondField = section.fields
                    .getOrNull(fieldIndex + 1)
                    ?.takeUnless { firstField.fullWidth || it.fullWidth }

                if (firstField.fullWidth) {
                    SingleLineFormFieldContent(
                        field = firstField,
                        item = item,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    fieldIndex += 1
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SingleLineFormFieldContent(
                            field = firstField,
                            item = item,
                            modifier = Modifier.weight(1f),
                        )
                        if (secondField != null) {
                            SingleLineFormFieldContent(
                                field = secondField,
                                item = item,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    fieldIndex += if (secondField == null) 1 else 2
                }
            }
        }
    }
}

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

package ru.pavlig43.product.internal.update.tabs.specification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Показывает карточную спецификацию товара и диалог результата генерации PDF.
 */
@Composable
@Suppress("LongMethod")
internal fun ProductSpecificationScreen(
    component: ProductSpecificationComponent,
) {
    val generationResult by component.generationResult.collectAsState()

    ProductSpecificationCards(component)

    generationResult?.let { result ->
        BasicAlertDialog(
            onDismissRequest = component::dismissGenerationResult,
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Start),
                    )
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Start),
                    )
                    Button(
                        onClick = component::dismissGenerationResult,
                    ) {
                        Text(if (result.isSuccess) "Ок" else "Закрыть")
                    }
                }
            }
        }
    }
}

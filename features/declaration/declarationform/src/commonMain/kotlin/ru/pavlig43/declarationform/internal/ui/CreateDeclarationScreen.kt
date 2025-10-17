package ru.pavlig43.declarationform.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.declarationform.internal.component.CreateDeclarationComponent
import ru.pavlig43.upsertitem.api.ui.CreateStateScreen

@Composable
internal fun  CreateDeclarationScreen(
    createItemComponent: CreateDeclarationComponent,
    modifier: Modifier = Modifier
){
    Column(modifier.fillMaxSize()) {
        DeclarationRequireScreen(createItemComponent.requires)
        CreateStateScreen(createItemComponent.createComponent)
    }

}
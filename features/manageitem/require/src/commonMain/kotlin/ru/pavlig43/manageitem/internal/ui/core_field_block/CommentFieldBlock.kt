package ru.pavlig43.manageitem.internal.ui.core_field_block

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.coreui.StringColumnField
import ru.pavlig43.manageitem.internal.ui.NAME

@Composable
internal fun CommentFieldBlock(
    comment: String,
    onChangeComment:(String)-> Unit,
    modifier: Modifier = Modifier
){
    StringColumnField(
        value = comment,
        onValueChange = onChangeComment,
        headText = NAME,
        modifier = modifier
    )


}
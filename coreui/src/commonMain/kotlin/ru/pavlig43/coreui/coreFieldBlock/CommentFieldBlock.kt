package ru.pavlig43.coreui.coreFieldBlock

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.coreui.StringColumnField

@Composable
fun CommentFieldBlock(
    comment: String,
    onChangeComment:(String)-> Unit,
    modifier: Modifier = Modifier
){
    StringColumnField(
        value = comment,
        onValueChange = onChangeComment,
        headText = "Комментарий",
        modifier = modifier
    )


}
package ru.pavlig43.rootnocombro.internal.navigation.drawer.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NavigationDrawer(modifier: Modifier) {
    ModalNavigationDrawer(
        drawerContent = {},
        modifier = modifier
    ) {

    }
}

@Composable
private fun<NavigationConfiguration : Any> DrawerContent(
    items: List<NavigationConfiguration>,
    modifier: Modifier) {
    val listState = rememberLazyListState()
    ModalDrawerSheet(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
            items(items){
                item ->
            }
        }
    }

}
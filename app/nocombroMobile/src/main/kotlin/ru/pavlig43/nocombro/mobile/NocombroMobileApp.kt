package ru.pavlig43.nocombro.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import ru.pavlig43.nocombro.mobile.experiments.ExperimentsRoute
import ru.pavlig43.nocombro.mobile.navigation.MobileChild
import ru.pavlig43.nocombro.mobile.navigation.NocombroMobileRootComponent

/**
 * Root Compose-обвязка mobile-приложения.
 */
@Composable
fun NocombroMobileApp(
    component: NocombroMobileRootComponent,
) {
    MaterialTheme(

    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            NocombroMobileContent(component)
        }
    }
}

@Composable
private fun NocombroMobileContent(
    component: NocombroMobileRootComponent,
) {
    Children(
        stack = component.stack,
        modifier = Modifier.fillMaxSize(),
    ) { child ->
        when (val instance = child.instance) {
            MobileChild.Menu -> MainMenuScreen(component)
            is MobileChild.Experiments -> ExperimentsRoute(
                component = instance.component,
                onOpenMenu = component::openMenu,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainMenuScreen(
    component: NocombroMobileRootComponent,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {MainTopBar()},
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(component.menuItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { component.selectMenuItem(item.config) },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class) // Разрешаем использовать TopAppBar из Material3
@Composable // Это composable-функция
fun MainTopBar(modifier: Modifier = Modifier) { // Отдельный AppBar
    TopAppBar( // Верхняя панель
        modifier = modifier.fillMaxWidth(), // TopAppBar на всю ширину
        colors = TopAppBarDefaults.topAppBarColors( // Настройка цветов панели
            containerColor = MaterialTheme.colorScheme.background // Фон самой панели
        ),
        title = { // Контент заголовка
            Card( // Карточка внутри AppBar
                modifier = Modifier // Начинаем модификаторы карточки
                    .fillMaxWidth() // Карточка на всю доступную ширину
                    .padding(end = 16.dp), // Отступ справа, чтобы не липла к краю
                shape = RoundedCornerShape(18.dp), // Скругление углов
                colors = CardDefaults.cardColors( // Цвет карточки
                    containerColor = MaterialTheme.colorScheme.primaryContainer // Красивый цвет из темы
                ),
                elevation = CardDefaults.cardElevation(2.dp) // Небольшая тень
            ) {
                Text( // Текст внутри карточки
                    text = "Nocombro", // Сам текст
                    modifier = Modifier // Модификаторы текста
                        .fillMaxWidth() // Текстовый блок на всю ширину карточки
                        .padding(vertical = 10.dp), // Вертикальный внутренний отступ
                    textAlign = TextAlign.Center, // Текст по центру
                    style = MaterialTheme.typography.titleMedium, // Стиль текста
                    fontWeight = FontWeight.Bold, // Жирный текст
                    color = MaterialTheme.colorScheme.onPrimaryContainer // Цвет текста под фон карточки
                )
            }
        }
    )
}

package ru.pavlig43.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Компонент основной вкладки приложения(это те, которые на главном экране и динамические).
 * Каждая реализация представляет отдельную вкладку (например, "Список документов", "Модификация документа")
 * и должна предоставлять модель данных для отображения.
 * Реализация это интерфейса будет в конструкторе MainTabChild, который будет находиться в модуле,
 * где основная навигация приложения по вкладкам
 * (сейчас rootnocombro/src/commonMain/kotlin/ru/pavlig43/rootnocombro/internal/navigation/MainTabNavigationComponent.kt)
 */
interface MainTabComponent {

    /**
     * Поток состояния вкладки, содержащий данные для отображения.
     *
     * @see NavTabState
     */
    val model: StateFlow<NavTabState>

    /**
     * Модель данных основной вкладки, которая наверху с крестиком.
     *
     * @property title Заголовок вкладки для отображения в UI
     */
    data class NavTabState(
        val title: String,
    )
}



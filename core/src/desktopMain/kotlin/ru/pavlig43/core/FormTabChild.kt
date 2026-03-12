package ru.pavlig43.core

import kotlinx.coroutines.flow.Flow

/**
 * Дочерний элемент вкладки формы.
 * Представляет обертку вокруг компонента формы для навигации по вкладкам.
 *
 * Используется в системах с динамическими вкладками, где каждая вкладка содержит
 * независимую форму/компонент (например, многошаговые формы, редактирование документов).
 *
 * @property component Компонент формы, который содержит бизнес-логику и UI состояние
 */
interface FormTabChild {
    val component: FormTabComponent
}

/**
 * Компонент формы, отображаемый во вкладке.
 * Определяет контракт для всех компонентов, которые могут быть размещены в табах.
 * @property title Заголовок вкладки для отображения в UI
 * @property onUpdate Асинхронное обновление данных формы
 * @property errorMessages Поток ошибок валидации для отображения в UI
 */
interface FormTabComponent {
    val title: String
    suspend fun onUpdate(): Result<Unit>
    val errorMessages: Flow<List<String>>
}
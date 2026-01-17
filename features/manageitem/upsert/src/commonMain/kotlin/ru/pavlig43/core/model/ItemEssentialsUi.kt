package ru.pavlig43.core.model

/**
 * Каждый объект(документ, продукт) при создании и редактировании
 * имеет поля для заполнения(имя, тип)
 * наследуется от этого интерфейса
 */
interface ItemEssentialsUi{
    val id: Int
}
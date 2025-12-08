package ru.pavlig43.itemlist.api.data

interface IItemUi {
    val id: Int
    //TODO удалить этот параметр,он есть не у всех
    val displayName: String
}
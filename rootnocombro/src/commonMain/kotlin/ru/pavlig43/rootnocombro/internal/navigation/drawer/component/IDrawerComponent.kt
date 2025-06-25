package ru.pavlig43.rootnocombro.internal.navigation.drawer.component

interface IDrawerComponent<DrawerConfiguration : Any> {
    fun onSelect(configuration: DrawerConfiguration)
}


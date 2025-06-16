package ru.pavlig43.rootnocombro.intetnal.di

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GlobalTabState {
    private val _tabs = mutableStateListOf<TabScreen>()
    val tabs: List<TabScreen>
        get() = _tabs
    private val _activeTab = MutableStateFlow(0)
    val activeTab = _activeTab.asStateFlow()

}
interface TabScreen{
    val title: String
    val isActive: Boolean
}


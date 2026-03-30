package ru.pavlig43.coreui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

object KeyEventHandler {
    private val handlers = mutableListOf<(KeyEvent) -> Boolean>()

    fun subscribe(handler: (KeyEvent) -> Boolean) {
        handlers += handler
    }

    fun unsubscribe(handler: (KeyEvent) -> Boolean) {
        handlers -= handler
    }

    fun handle(event: KeyEvent): Boolean {
        return handlers.reversed().any { it(event) }
    }


}
val KeyEvent.isEscKeyUp
    get() =  key == Key.Escape && type == KeyEventType.KeyUp
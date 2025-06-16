package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import ru.pavlig43.document.api.component.IDocumentComponent
import ru.pavlig43.signroot.api.component.IRootSignComponent

interface IRootNocombroComponent {
    val stack: Value<ChildStack<*, Child>>



    sealed interface Child{
        class RootSign(val component: IRootSignComponent): Child
        class Document(val component: IDocumentComponent): Child
        class Tab(): Child
    }
}
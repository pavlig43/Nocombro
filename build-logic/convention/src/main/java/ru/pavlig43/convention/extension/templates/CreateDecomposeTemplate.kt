package ru.pavlig43.convention.extension.templates

import java.io.File


internal fun createComponents(
    outerDir: String,
    moduleName: String,
    outerPackageName: String
) {
    val packageName = "$outerPackageName.api.component"
    val dir = "$outerDir/api/component"

    val dirFolder = File(dir)

    dirFolder.mkdirs()

    val pascalName = moduleName.toPascalCase()
    val classState = "${pascalName}State"

    createIComponent(
        pascalName, packageName, dir, classState
    )
    createComponent(pascalName, packageName, dir, classState)

}

private fun createIComponent(
    pascalName: String,
    packageName: String,
    dir: String,
    classState: String
) {
    val componentInterfaceFile = File(dir, "I${pascalName}Component.kt")
    componentInterfaceFile.mkdirs()
    componentInterfaceFile.writeText(
        """
        package $packageName

        import kotlinx.coroutines.flow.StateFlow
        
        interface I${pascalName}Component {

            val ${classState.toCamelCase()}: StateFlow<$classState>

        }
        interface $classState {
            class Initial : $classState
            class Loading : $classState
            class Success(val data:) : $classState
            class Error(val message: String) : $classState
        }

    """.trimIndent()
    )
}

private fun createComponent(
    pascalName: String,
    packageName: String,
    dir: String,
    classState: String
) {
    val componentFile = File(dir, "${pascalName}Component.kt")
    componentFile.mkdirs()
    componentFile.writeText(
        """
        package $packageName
        
        import com.arkivanov.decompose.ComponentContext
        import com.arkivanov.decompose.childContext
        import com.arkivanov.essenty.instancekeeper.getOrCreate
        import kotlinx.coroutines.flow.MutableStateFlow
        import kotlinx.coroutines.flow.asStateFlow
        import kotlinx.coroutines.flow.update
        import org.koin.core.scope.Scope

        import ru.pavlig43.core.RequestResult
        import ru.pavlig43.core.componentCoroutineScope
        import ru.pavlig43.corekoin.ComponentKoinContext
        
        class ${pascalName}Component(
            componentContext: ComponentContext,
        ): ComponentContext by componentContext, ${pascalName}Component {

            private val coroutineScope = componentCoroutineScope()
            private val koinContext = instanceKeeper.getOrCreate {
                ComponentKoinContext()
            }
            private val scope: Scope =
                koinContext.getOrCreateKoinScope()

            private val _${classState.toCamelCase()} = MutableStateFlow<$classState>($classState.Initial())
            
            override val ${classState.toCamelCase()} = _${classState.toCamelCase()}.asStateFlow()
           

        }

    """.trimIndent()
    )
}




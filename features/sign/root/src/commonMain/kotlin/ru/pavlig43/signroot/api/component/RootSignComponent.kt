package ru.pavlig43.signroot.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.serialization.Serializable
import ru.pavlig43.signsignin.api.component.SignInComponent
import ru.pavlig43.signsignup.api.component.SignUpComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.signroot.api.IRootSignDependencies
import ru.pavlig43.signroot.internal.di.createRootSignModule


class RootSignComponent(
    componentContext: ComponentContext,
    rootSignDependencies: IRootSignDependencies,
    private val signIn: () -> Unit,
    private val signUp: () -> Unit,
) : IRootSignComponent, ComponentContext by componentContext {


    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(createRootSignModule(rootSignDependencies))

    private val stackNavigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<Config, IRootSignComponent.Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.SignUp,
        handleBackButton = false,
        childFactory = ::createChild
    )


    private fun createChild(
        signConfig: Config,
        componentContext: ComponentContext
    ): IRootSignComponent.Child {
        return when (signConfig) {

            Config.SignIn -> IRootSignComponent.Child.SignIn(
                SignInComponent(
                    componentContext = componentContext,
                    signInDependencies = scope.get(),
                    navigateToSignUp = {
                        stackNavigation.pushToFront(Config.SignUp)
                    },
                    signIn = signIn


                )
            )
            Config.SignUp -> IRootSignComponent.Child.SignUp(
                SignUpComponent(
                    componentContext = componentContext,
                    signUpDependencies = scope.get(),
                    navigateToSignIn = { stackNavigation.pushToFront(Config.SignIn) },
                    signUp = { signUp() }

                )
            )
        }
    }
    @Serializable
    sealed interface Config {
        @Serializable
        data object SignIn : Config

        @Serializable
        data object SignUp : Config

    }

}


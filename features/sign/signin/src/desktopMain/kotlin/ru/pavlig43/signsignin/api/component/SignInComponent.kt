package ru.pavlig43.signsignin.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.scope.Scope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.signcommon.logopass.api.components.ILogoPassComponent
import ru.pavlig43.signcommon.logopass.api.components.LogoPassComponent
import ru.pavlig43.signcommon.social.api.components.ISocialSignComponent
import ru.pavlig43.signcommon.social.api.components.SocialSignComponent
import ru.pavlig43.signsignin.api.ISignInDependencies
import ru.pavlig43.signsignin.internal.di.createSignInModule

class SignInComponent(
    componentContext: ComponentContext,
    signInDependencies: ISignInDependencies,
    private val signIn: () -> Unit,
    private val navigateToSignUp: () -> Unit,
) : ComponentContext by componentContext, ISignInComponent {
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createSignInModule(signInDependencies))

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Initial)

    override val signInState = _signInState.asStateFlow()

    private fun handleSignInState(signInState: SignInState) {
        when (signInState) {
            is SignInState.Error -> {
                println("error ${signInState.message}")
            }

            is SignInState.Initial -> {}
            is SignInState.Loading -> {
                println("loading signIn")
            }
            is SignInState.Success -> signIn()
        }
    }

    private fun trySignIn(result: Result<*>) {
        val state = result.fold(
            onSuccess = { SignInState.Success },
            onFailure = { SignInState.Error(it.message ?: "Неизвестная ошибка")}
        )
        _signInState.update { state }
        handleSignInState(_signInState.value)
    }



    override fun onSignUpClick() {
        navigateToSignUp()
    }


    /**
     * TODO() проверить на пересоздание репозитория при повороте
     *  private val logoPassRepository:ILogoPassRepository = instanceKeeper.getOrCreate { scope.get() }
     */

    override val logoPassComponent: ILogoPassComponent = LogoPassComponent(
        componentContext = childContext("logoPass"),
        logoPassRepository = scope.get(),
        sendLogoPassRequest = { result -> trySignIn(result) },
    )
    override val socialSignComponent: ISocialSignComponent =
        SocialSignComponent(componentContext = childContext("social"))


}







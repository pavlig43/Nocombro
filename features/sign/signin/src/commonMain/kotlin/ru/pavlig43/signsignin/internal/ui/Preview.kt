package ru.pavlig43.signsignin.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.pavlig43.AppForPreview
import ru.pavlig43.signcommon.logopass.api.components.FakeLogoPassComponent
import ru.pavlig43.signcommon.logopass.api.components.ILogoPassComponent
import ru.pavlig43.signcommon.social.api.components.FakeSocialSignComponent
import ru.pavlig43.signcommon.social.api.components.ISocialSignComponent
import ru.pavlig43.signsignin.api.component.ISignInComponent
import ru.pavlig43.signsignin.api.component.SignInState
import ru.pavlig43.signsignin.api.ui.SignInScreen


@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun LogoPassFormPreviewDark() {
    AppForPreview(true) {
        SignInScreen(
            FakeSignInComponent()
        )
    }

}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun LogoPassFormPreviewLight() {
    AppForPreview(false) {
        SignInScreen(
            FakeSignInComponent()
        )
    }
}
private class FakeSignInComponent: ISignInComponent {
    override val logoPassComponent: ILogoPassComponent = FakeLogoPassComponent()
    override val socialSignComponent: ISocialSignComponent = FakeSocialSignComponent()
    override val signInState: StateFlow<SignInState> = MutableStateFlow(SignInState.Initial)
    override fun onSignUpClick() = Unit

}
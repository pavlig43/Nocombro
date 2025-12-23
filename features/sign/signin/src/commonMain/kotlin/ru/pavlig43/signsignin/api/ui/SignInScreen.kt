package ru.pavlig43.signsignin.api.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.signcommon.logopass.api.ui.LogoPassForm
import ru.pavlig43.signcommon.social.api.ui.SocialForm
import ru.pavlig43.signcommon.ui.NavigateOtherScreenButton
import ru.pavlig43.signsignin.api.component.ISignInComponent
import ru.pavlig43.signsignin.internal.ui.COMPONENT_WIDTH
import ru.pavlig43.signsignin.internal.ui.ENTER
import ru.pavlig43.signsignin.internal.ui.LOGIN_IN_ACCOUNT
import ru.pavlig43.signsignin.internal.ui.REGISTER


@Composable
fun SignInScreen(
    signInComponent: ISignInComponent,
    modifier: Modifier = Modifier
) {

    val scrollState = rememberScrollState()

    Column(
        modifier.fillMaxHeight().width(COMPONENT_WIDTH.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LogoPassForm(
            logoPassComponent = signInComponent.logoPassComponent,
            titleScreen = LOGIN_IN_ACCOUNT,
            logoPassButtonText = ENTER
        )
        Spacer(modifier = Modifier.height(48.dp))

        SocialForm(
//            signInComponent.socialSignComponent
        )

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.weight(1f))

        NavigateOtherScreenButton(
            navigate = signInComponent::onSignUpClick,
            navigateButtonText = REGISTER
        )

    }
}


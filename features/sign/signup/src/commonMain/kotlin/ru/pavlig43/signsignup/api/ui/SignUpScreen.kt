package ru.pavlig43.signsignup.api.ui

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
import ru.pavlig43.signsignup.api.component.ISignUpComponent
import ru.pavlig43.signsignup.internal.ui.ALREADY_HAVE_ACCOUNT
import ru.pavlig43.signsignup.internal.ui.COMPONENT_WIDTH
import ru.pavlig43.signsignup.internal.ui.REGISTER
import ru.pavlig43.signsignup.internal.ui.REGISTRATION

@Composable
fun SignUpScreen(
    signUpComponent: ISignUpComponent,
    modifier: Modifier = Modifier
) {

    val scrollState = rememberScrollState()

    Column(
        modifier.fillMaxHeight().width(COMPONENT_WIDTH.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LogoPassForm(
            logoPassComponent = signUpComponent.logoPassComponent,
            titleScreen = REGISTRATION,
            logoPassButtonText = REGISTER
        )
        Spacer(modifier = Modifier.height(48.dp))

        SocialForm(
//            signUpComponent.socialSignComponent
        )

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.weight(1f))

        NavigateOtherScreenButton(
            navigate = signUpComponent::onSignInClick,
            navigateButtonText = ALREADY_HAVE_ACCOUNT
        )

    }
}


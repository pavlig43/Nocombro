package ru.pavlig43.signcommon.social.api.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nocombro.features.sign.common.generated.resources.Res
import nocombro.features.sign.common.generated.resources.sign_with

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.pavlig43.signcommon.social.api.components.ISocialSignComponent
import ru.pavlig43.signcommon.social.api.data.SocialItem


@Composable
fun SocialForm(
    socialSignComponent: ISocialSignComponent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = SIGN_WITH,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,

        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SocialItem.entries.forEach {
                SocialButton(it)
            }
        }
    }


}

@Composable
private fun SocialButton(
    socialItem: SocialItem,
    signWithSocial: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier
            .width(88.dp)
            .height(56.dp)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f))
            .clickable { signWithSocial() },
    ) {
        Box(Modifier.fillMaxSize()) {
            Icon(
                painterResource(socialItem.icon),
                socialItem.contentDescription,
                Modifier.align(Alignment.Center).size(32.dp)
            )
        }

    }
}
private const val SIGN_WITH = "Присоединиться с"
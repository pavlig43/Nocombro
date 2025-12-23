package ru.pavlig43.signcommon.social.api.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.signcommon.social.api.data.SocialItem


@Composable
fun SocialForm(
//    socialSignComponent: ISocialSignComponent,
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
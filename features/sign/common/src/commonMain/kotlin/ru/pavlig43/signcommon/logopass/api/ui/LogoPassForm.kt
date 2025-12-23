package ru.pavlig43.signcommon.logopass.api.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ru.pavlig43.signcommon.logopass.api.components.ILogoPassComponent


@Composable
fun LogoPassForm(
    logoPassComponent: ILogoPassComponent,
    titleScreen: String,
    logoPassButtonText: String
) {

    val login by logoPassComponent.login.collectAsState()
    val password by logoPassComponent.password.collectAsState()


    var isVisiblePassword by remember { mutableStateOf(false) }


    val textFieldModifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = titleScreen,

            style = MaterialTheme.typography.headlineSmall,
            fontStyle = FontStyle.Italic
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = login,
            onValueChange = { logoPassComponent.onLoginChanged(it) },
            placeholder = { Text("Login", style = MaterialTheme.typography.bodyMedium) },
            modifier = textFieldModifier
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { logoPassComponent.onPasswordChanged(it) },
            placeholder = { Text("Password", style = MaterialTheme.typography.bodyMedium) },
            visualTransformation = if (isVisiblePassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton({ isVisiblePassword = !isVisiblePassword }) {
                    Icon(
                        imageVector = if (isVisiblePassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isVisiblePassword) "Hide password" else "Show password"
                    )
                }
            },
            modifier = textFieldModifier
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = logoPassComponent::sendLogoPass,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Text(text = logoPassButtonText)

        }

    }
}




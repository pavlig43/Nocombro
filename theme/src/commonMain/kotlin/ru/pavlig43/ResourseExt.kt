package ru.pavlig43

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
@NonSkippableComposable
public fun DrawableResource.vector(): ImageVector = vectorResource(this)

@Composable
@NonSkippableComposable
public fun DrawableResource.painter(): Painter = painterResource(this)

@Composable
@NonSkippableComposable
public fun DrawableResource.image(): ImageBitmap = imageResource(this)

@Composable
@NonSkippableComposable
public fun StringArrayResource.strings(): List<String> = stringArrayResource(this)

@Composable
@NonSkippableComposable
public fun StringResource.string(
    vararg args: Any,
    trim: Boolean = true
): String = stringResource(this, formatArgs = args).let { if (trim) it.trim() else it }

@Composable
@NonSkippableComposable
public fun PluralStringResource.plural(
    quantity: Int,
    vararg args: Any
): String = pluralStringResource(this, quantity, formatArgs = args)






@Composable
@NonSkippableComposable
public fun FontResource.font(
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = Font(this, weight, style)

public suspend fun StringResource.getString(
    vararg args: Any,
    trim: Boolean = true
): String = org.jetbrains.compose.resources.getString(this, formatArgs = args).let { if (trim) it.trim() else it }
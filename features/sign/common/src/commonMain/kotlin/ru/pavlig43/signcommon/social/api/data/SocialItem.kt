package ru.pavlig43.signcommon.social.api.data


import nocombro.features.sign.common.generated.resources.Res
import nocombro.features.sign.common.generated.resources.apple
import nocombro.features.sign.common.generated.resources.google
import nocombro.features.sign.common.generated.resources.sign_with_apple
import nocombro.features.sign.common.generated.resources.sign_with_google
import nocombro.features.sign.common.generated.resources.sign_with_vk
import nocombro.features.sign.common.generated.resources.vk
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class SocialItem(val icon: DrawableResource, val contentDescription: String) {
    VK(Res.drawable.vk, SIGN_WITH_VK),
    GOOGLE(Res.drawable.google, SIGN_WITH_GOOGLE),
    APPLE(Res.drawable.apple, SIGN_WITH_APPLE)
}

private const val SIGN_WITH_VK = "Присоединиться с Вконтакте"
private const val SIGN_WITH_APPLE = "Присоединиться с Эпл"
private const val SIGN_WITH_GOOGLE = "Присоединиться с Гугл"

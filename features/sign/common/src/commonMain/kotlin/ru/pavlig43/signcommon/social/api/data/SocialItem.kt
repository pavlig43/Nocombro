package ru.pavlig43.signcommon.social.api.data



import org.jetbrains.compose.resources.DrawableResource
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.apple
import ru.pavlig43.theme.google
import ru.pavlig43.theme.vk

enum class SocialItem(val icon: DrawableResource, val contentDescription: String) {
    VK(Res.drawable.vk, SIGN_WITH_VK),
    GOOGLE(Res.drawable.google, SIGN_WITH_GOOGLE),
    APPLE(Res.drawable.apple, SIGN_WITH_APPLE)
}

private const val SIGN_WITH_VK = "Присоединиться с Вконтакте"
private const val SIGN_WITH_APPLE = "Присоединиться с Эпл"
private const val SIGN_WITH_GOOGLE = "Присоединиться с Гугл"

package ru.pavlig43.signcommon.social.api.components

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.signcommon.social.api.components.ISocialSignComponent

class SocialSignComponent(
    componentContext: ComponentContext,
):ComponentContext by componentContext, ISocialSignComponent
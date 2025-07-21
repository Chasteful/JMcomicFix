package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice


class ComponentMode(
    quality: QUALITY,
) : Configurable("GraphicQuality") {
    companion object {
        fun quality() = ComponentMode(QUALITY.FANCY)
    }

    @Suppress("unused")
    val quality by enumChoice("Quality", quality)

}

@Suppress("unused")
enum class QUALITY(override val choiceName: String) : NamedChoice {
    POTATO("Potato"),
    FAST("Fast"),
    FANCY("Fancy"),
    COMPACT("Compact"),

}

package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

object ModuleIQBoost : ClientModule("IQBoost", Category.FUN) {
    @Suppress("unused")
    private val iq by int("Amount", 1337, 1..114514, "IQ").also {
        tagBy(this)
    }
}

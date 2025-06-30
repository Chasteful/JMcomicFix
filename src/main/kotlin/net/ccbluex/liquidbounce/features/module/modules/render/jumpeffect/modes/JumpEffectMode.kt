package net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.ModuleJumpEffect.modes

abstract class JumpEffectMode(
    name: String
) : Choice(name) {
    override val parent
        get() = modes
}

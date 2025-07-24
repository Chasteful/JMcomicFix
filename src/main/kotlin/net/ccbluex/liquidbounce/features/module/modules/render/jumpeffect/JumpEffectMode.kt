package net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect

import net.ccbluex.liquidbounce.config.types.nesting.Choice

abstract class JumpEffectMode(
    name: String
) : Choice(name) {
    override val parent
        get() = ModuleJumpEffect.modes
}

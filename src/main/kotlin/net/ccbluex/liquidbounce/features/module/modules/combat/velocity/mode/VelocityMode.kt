package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity

abstract class VelocityMode(name: String) : Choice(name) {

    override val parent: ChoiceConfigurable<VelocityMode>
        get() = ModuleVelocity.modes

    override val running: Boolean
        get() = super.running && ModuleVelocity.pause == 0

}

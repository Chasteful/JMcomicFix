package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.fluid

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerFluidCollisionCheckEvent
import net.ccbluex.liquidbounce.event.handler

internal class NoSlowFluidNone(override val parent: ChoiceConfigurable<*>) : Choice("None") {

    @Suppress("unused")
    private val fluidCollisionHandler = handler<PlayerFluidCollisionCheckEvent> {
        it.cancelEvent()
    }
}

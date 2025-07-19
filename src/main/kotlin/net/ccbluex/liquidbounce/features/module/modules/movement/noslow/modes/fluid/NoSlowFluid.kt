package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.fluid

import net.ccbluex.liquidbounce.event.events.FluidPushEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.NoSlowUseActionHandler
import net.ccbluex.liquidbounce.utils.client.inGame

object NoSlowFluid : NoSlowUseActionHandler("Fluid") {

    @Suppress("unused")
    private val mode = choices(this, "Mode", 0) {
        arrayOf(
            NoSlowFluidNone(it),
            NoSlowFluidOldGrimAC(it)
        )
    }

    init {
        handler<FluidPushEvent> {
            it.cancelEvent()
        }
    }

    override val running: Boolean
        get() = super.running && inGame
}

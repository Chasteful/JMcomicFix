package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.intave

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority

class SpeedIntave(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Intave", parent) {
    private val mode by enumChoice("Mode", IntaveMode.INTAVE14)
    private val timer by boolean("Timer", true)

    private inner class Strafe(parent: EventListener) : ToggleableConfigurable(parent, "Strafe", true) {
        private val strength by float("Strength", 0.27f, 0.01f..0.27f)

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.isSprinting && (player.isOnGround || player.airTicks == 11)) {
                player.velocity = player.velocity.withStrafe(strength = strength.toDouble())
            }
        }
    }

    private inner class AirBoost(parent: EventListener) : ToggleableConfigurable(parent, "AirBoost", true) {
        private val boostConstant = 0.003

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.velocity.y > 0.003 && player.isSprinting) {
                player.velocity.x *= 1f + (boostConstant * 0.25)
                player.velocity.z *= 1f + (boostConstant * 0.25)
            }
        }
    }

    init {
        // Only initialize Intave14 components when needed
        if (mode == IntaveMode.INTAVE14) {
            tree(Strafe(this))
            tree(AirBoost(this))
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        when (mode) {
            IntaveMode.INTAVE14_FAST -> handleIntave14FastTick()
            IntaveMode.INTAVE14 -> {} // Handled by sub-configurables
        }
    }

    private fun handleIntave14FastTick() {
        when (player.airTicks) {
            1 -> {
                player.velocity.x *= 1.04
                player.velocity.z *= 1.04
            }

            2, 3, 4 -> {
                player.velocity.x *= 1.02
                player.velocity.z *= 1.02
            }
        }

        if (timer) {
            Timer.requestTimerSpeed(1.002f, Priority.NOT_IMPORTANT, ModuleSpeed)
        }
    }

    enum class IntaveMode(override val choiceName: String) : NamedChoice {
        INTAVE14("Intave14"),
        INTAVE14_FAST("Intave14Fast")
    }
}

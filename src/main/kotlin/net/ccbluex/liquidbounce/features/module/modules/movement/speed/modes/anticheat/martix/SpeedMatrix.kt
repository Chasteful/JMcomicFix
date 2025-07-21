package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.martix

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority

class SpeedMatrix(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Matrix", parent) {

    private val mode by enumChoice("Mode", MatrixMode.MATRIX_7)

    private var wasTimer = false

    @Suppress("unused")
    private val tickHandler = tickHandler {
        when (mode) {
            MatrixMode.MATRIX_7 -> handleMatrix7()
            MatrixMode.MATRIX_692 -> handleMatrix692()
        }
    }

    private fun handleMatrix7() {
        if (player.moving) {
            when {
                player.isOnGround -> {
                    player.velocity.y = 0.419652
                    player.velocity = player.velocity.withStrafe()
                }

                player.sqrtSpeed < 0.04 -> {
                    player.velocity = player.velocity.withStrafe()
                }
            }
        }
    }

    private fun handleMatrix692() {
        if (wasTimer) {
            Timer.requestTimerSpeed(1.0f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
            wasTimer = false
        }

        if (player.moving && player.isOnGround) {
            Timer.requestTimerSpeed(1.35f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
            wasTimer = true
            player.velocity = player.velocity.withStrafe()
        } else if (player.sqrtSpeed < 0.215) {
            player.velocity = player.velocity.withStrafe(speed = 0.215)
        }
    }

    enum class MatrixMode(override val choiceName: String) : NamedChoice {
        MATRIX_7("Matrix7"),
        MATRIX_692("Matrix6.9.2")
    }
}

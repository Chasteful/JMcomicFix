package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.verus

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.MovementType

class SpeedVerus(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Verus", parent) {

    private val mode by enumChoice("Mode", VerusMode.LOW_HOP)
    private val strength by float("Strength", 0.33f, 0.1f..1f)

    private var wasOnGround = false

    override fun enable() {
        wasOnGround = false
        super.enable()
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        when (mode) {
            VerusMode.BHOP -> handleBhopMove(event)
            VerusMode.LOW_HOP -> {} // Handled in MovementInputEvent for LowHop
        }
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent> { event ->
        if (mode == VerusMode.LOW_HOP) {
            handleLowHopInput(event)
        }
    }

    @Suppress("unused")
    private val afterJumpHandler = handler<PlayerAfterJumpEvent> {
        if (mode == VerusMode.BHOP) {
            player.velocity.x *= 1.1
            player.velocity.z *= 1.1
        }
    }

    @Suppress("unused")
    private val timerHandler = tickHandler {
        if (mode == VerusMode.BHOP) {
            Timer.requestTimerSpeed(2.0F, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
            waitTicks(101)
        }
    }

    private fun handleBhopMove(event: PlayerMoveEvent) {
        if (event.type == MovementType.SELF && player.moving) {
            event.movement = event.movement.withStrafe(strength = 1.0)
        }
    }

    private fun handleLowHopInput(event: MovementInputEvent) {
        if (event.directionalInput.isMoving) {
            if (!player.isOnGround && !player.horizontalCollision) {
                player.velocity.y = -0.0784000015258789
            }
            player.velocity = player.velocity.withStrafe(strength = strength.toDouble())
        } else {
            player.velocity.x = 0.0
            player.velocity.z = 0.0
        }
    }

    enum class VerusMode(override val choiceName: String) : NamedChoice {
        LOW_HOP("VerusLowHop"),
        BHOP("VerusBhop")
    }
}

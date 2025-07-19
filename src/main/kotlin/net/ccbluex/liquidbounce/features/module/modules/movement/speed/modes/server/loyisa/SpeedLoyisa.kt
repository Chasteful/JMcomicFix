package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.server.loyisa

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

class SpeedLoyisa(override val parent: ChoiceConfigurable<*>) : Choice("Loyisa") {

    private val mode by enumChoice("Mode", LoyisaMode.INTAVE_12)

    private var stage = 0
    private var hasDamaged = false
    private var ticks = 0

    private fun getBaseMoveSpeed(): Double {
        var baseSpeed = 0.2873

        player.getStatusEffect(StatusEffects.SPEED)?.let {
            baseSpeed *= 1.0 + 0.2 * (it.amplifier + 1)
        }

        return baseSpeed
    }

    @Suppress("unused")
    private val moveInputHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (mode == LoyisaMode.INTAVE_12) {
            event.directionalInput = event.directionalInput.copy(forwards = true)
        }
    }

    @Suppress("unused")
    private val moveHandler = tickHandler {
        when (mode) {
            LoyisaMode.INTAVE_12 -> handleIntave12Move()
        }
    }

    private fun handleIntave12Move() {
        when {
            stage < 3 -> handleInitialStage()
            else -> handleDamageStage()
        }
    }

    private fun handleInitialStage() {
        player.velocity.x = 0.0
        player.velocity.z = 0.0

        if (player.isOnGround) {
            player.jump()
            stage++
            if (stage == 3 && player.isOnGround) applySelfDamage()
        }
    }

    private fun handleDamageStage() {
        if (player.hurtTime > 0 && !hasDamaged) {
            hasDamaged = true
        }

        if (!hasDamaged) return

        ticks++

        if (player.isOnGround) {
            handleGroundMovement()
        } else {
            player.velocity = player.velocity.withStrafe(0.0)
        }

        if (ticks > 16) {
            player.velocity = player.velocity.withStrafe(0.0)
            ModuleSpeed.enabled = false
        }
    }

    private fun handleGroundMovement() {
        player.velocity = player.velocity.withStrafe(getBaseMoveSpeed() * 3.2)
    }

    private fun applySelfDamage() {
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false, false))
        network.sendPacket(
            PlayerMoveC2SPacket.PositionAndOnGround(
                player.x, player.y + 3.25, player.z,
                false, false
            )
        )
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false, false))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, true, false))
    }

    override fun enable() {
        super.enable()
        stage = 0
        hasDamaged = false
        ticks = 0
    }

    enum class LoyisaMode(override val choiceName: String) : NamedChoice {
        INTAVE_12("Intave12")
    }
}

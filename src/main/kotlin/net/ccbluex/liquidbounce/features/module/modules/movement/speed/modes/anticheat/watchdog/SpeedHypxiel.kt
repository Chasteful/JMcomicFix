package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.watchdog

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.shape.VoxelShapes

class SpeedHypxiel(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Hypixel", parent) {

    private val mode by enumChoice("Mode", WatchdogMode.HYPIXEL_BHOP)

    // BHop specific settings
    private val horizontalAcceleration by boolean("HorizontalAccel", true)
    private val verticalAcceleration by boolean("VerticalAccel", true)

    // LowHop specific settings
    private val glide by boolean("Glide", false)

    companion object {
        // Shared constants
        private const val BASE_HORIZONTAL_MODIFIER = 0.0004
        private const val HORIZONTAL_SPEED_AMPLIFIER = 0.0007
        private const val VERTICAL_SPEED_AMPLIFIER = 0.0004
        private const val AT_LEAST = 0.281
        private const val BASH = 0.2857671997172534
        private const val SPEED_EFFECT_CONST = 0.008003278196411223

        // LowHop shared state
        var shouldStrafe = false
    }

    private var wasFlagged = false

    @Suppress("unused")
    private val tickHandler = tickHandler {
        when (mode) {
            WatchdogMode.HYPIXEL_BHOP -> handleBHopTick()
            WatchdogMode.HYPIXEL_LOWHOP -> handleLowHopTick()
        }
    }

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> {
        when (mode) {
            WatchdogMode.HYPIXEL_BHOP -> {
                val atLeast = if (!wasFlagged) {
                    AT_LEAST + SPEED_EFFECT_CONST * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)
                } else {
                    0.0
                }
                player.velocity = player.velocity.withStrafe(speed = player.sqrtSpeed.coerceAtLeast(atLeast))
            }

            WatchdogMode.HYPIXEL_LOWHOP -> {
                val atLeast = 0.247 + 0.15 * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)
                player.velocity = player.velocity.withStrafe(speed = player.sqrtSpeed.coerceAtLeast(atLeast))
                shouldStrafe = true
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (mode != WatchdogMode.HYPIXEL_BHOP) return@sequenceHandler

        val packet = event.packet
        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
            val velocityX = packet.velocityX / 8000.0
            val velocityY = packet.velocityY / 8000.0
            val velocityZ = packet.velocityZ / 8000.0

            waitTicks(1)

            val speed = if (velocityX == 0.0 && velocityZ == 0.0 && velocityY == -0.078375) {
                player.sqrtSpeed.coerceAtLeast(
                    BASH * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)
                )
            } else {
                player.sqrtSpeed
            }
            player.velocity = player.velocity.withStrafe(speed = speed)
        } else if (packet is PlayerPositionLookS2CPacket) {
            wasFlagged = true
        }
    }

    private fun handleBHopTick() {
        if (player.isOnGround) {
            player.velocity = player.velocity.withStrafe()
            return
        }

        val horizontalMod = if (horizontalAcceleration) {
            BASE_HORIZONTAL_MODIFIER + HORIZONTAL_SPEED_AMPLIFIER *
                (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)
        } else {
            0.0
        }

        val yMod = if (verticalAcceleration && player.velocity.y < 0 && player.fallDistance < 1) {
            VERTICAL_SPEED_AMPLIFIER
        } else {
            0.0
        }

        player.velocity = player.velocity.multiply(1.0 + horizontalMod, 1.0 + yMod, 1.0 + horizontalMod)
    }

    private fun handleLowHopTick() {
        shouldStrafe = false

        if (player.isOnGround) {
            player.velocity = player.velocity.withStrafe()
            shouldStrafe = true
            return
        }

        when (player.airTicks) {
            1 -> {
                player.velocity = player.velocity.withStrafe()
                shouldStrafe = true
                player.velocity.y += 0.0568
            }

            3 -> {
                player.velocity.x *= 0.95
                player.velocity.y -= 0.13
                player.velocity.z *= 0.95
            }

            4 -> player.velocity.y -= 0.2
            7 -> {
                if (glide && isGroundExempt()) {
                    player.velocity.y = 0.0
                }
            }
        }

        if (isGroundExempt()) {
            player.velocity = player.velocity.withStrafe()
        }

        if (player.hurtTime == 9) {
            player.velocity = player.velocity.withStrafe(speed = player.sqrtSpeed.coerceAtLeast(0.281))
        }

        if ((player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) == 2) {
            when (player.airTicks) {
                1, 2, 5, 6, 8 -> player.velocity = player.velocity.multiply(1.2, 1.0, 1.2)
            }
        }
    }

    private fun isGroundExempt() =
        world.getBlockCollisions(player, player.boundingBox.offset(0.0, -0.66, 0.0)).any { shape ->
            shape != VoxelShapes.empty()
        } && player.velocity.y < 0

    override fun disable() {
        wasFlagged = false
        shouldStrafe = false
    }

    enum class WatchdogMode(override val choiceName: String) : NamedChoice {
        HYPIXEL_BHOP("HypixelBHop"),
        HYPIXEL_LOWHOP("HypixelLowHop")
    }
}

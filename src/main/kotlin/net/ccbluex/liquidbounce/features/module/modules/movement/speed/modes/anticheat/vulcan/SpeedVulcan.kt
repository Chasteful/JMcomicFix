package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.vulcan


import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.abs

class SpeedVulcan(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Vulcan", parent) {

    private val mode by enumChoice("Mode", VulcanMode.VULCAN_GROUND_286)

    // Common properties and handlers
    private inline val goingSideways: Boolean
        get() = player.input.movementSideways != 0f

    @Suppress("unused")
    private val afterJumpHandler = sequenceHandler<PlayerAfterJumpEvent> {
        when (mode) {
            VulcanMode.VULCAN_286 -> handleVulcan286Jump()
            VulcanMode.VULCAN_288 -> handleVulcan288Jump()
            else -> { /* Not handled in this sequence */
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        when (mode) {
            VulcanMode.VULCAN_GROUND_286 -> handleVulcanGround286Tick()
            VulcanMode.VULCAN_288 -> handleVulcan288Tick()
            else -> { /* Not handled in tick */
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (mode) {
            VulcanMode.VULCAN_GROUND_286 -> handleVulcanGround286Packet(event)
            VulcanMode.VULCAN_288 -> handleVulcan288Packet(event)
            else -> { /* Not handled */
            }
        }
    }

    @Suppress("unused")
    private val jumpEvent = handler<PlayerJumpEvent> { event ->
        if (mode == VulcanMode.VULCAN_GROUND_286 && !mc.options.jumpKey.isPressed) {
            event.cancelEvent()
        }
    }

    private suspend fun Sequence.handleVulcan286Jump() {
        val speedLevel = (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)
        waitTicks(1)
        player.velocity =
            player.velocity.withStrafe(speed = if (goingSideways) 0.3345 else 0.3355 * (1 + speedLevel * 0.3819))
        waitTicks(1)
        if (player.isSprinting) {
            player.velocity =
                player.velocity.withStrafe(speed = if (goingSideways) 0.3235 else 0.3284 * (1 + speedLevel * 0.355))
        }
        waitTicks(2)
        player.velocity = player.velocity.copy(y = -0.376)
        waitTicks(2)
        if (player.speed > 0.298) {
            player.velocity = player.velocity.withStrafe(speed = 0.298)
        }
    }

    private suspend fun Sequence.handleVulcan288Jump() {
        val hasSpeed = (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) != 0
        player.velocity = player.velocity.withStrafe(speed = if (hasSpeed) 0.771 else 0.5)
        waitTicks(1)
        player.velocity = player.velocity.withStrafe(speed = if (hasSpeed) 0.605 else 0.31)
        waitTicks(1)
        player.velocity = player.velocity.withStrafe(speed = if (hasSpeed) 0.57 else 0.29)
        player.velocity = player.velocity.copy(y = if (hasSpeed) -0.5 else -0.37)
        waitTicks(1)
        player.velocity = player.velocity.withStrafe(speed = if (hasSpeed) 0.595 else 0.27)
        waitTicks(1)
        player.velocity = player.velocity.withStrafe(speed = if (hasSpeed) 0.595 else 0.28)
    }

    private fun handleVulcan288Tick() {
        val hasSpeed = (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) != 0
        if (!player.isOnGround && abs(player.fallDistance) > 0 && hasSpeed) {
            player.velocity.x *= 1.055
            player.velocity.z *= 1.055
        }
    }

    private fun handleVulcanGround286Tick() {
        if (player.moving && collidesBottomVertical() && !mc.options.jumpKey.isPressed) {
            val speedEffect = player.getStatusEffect(StatusEffects.SPEED)
            val isAffectedBySpeed = speedEffect != null && speedEffect.amplifier > 0
            val isMovingSideways = player.input.movementSideways != 0f

            val strafe = when {
                isAffectedBySpeed -> 0.59
                isMovingSideways -> 0.41
                else -> 0.42
            }

            player.velocity = player.velocity.withStrafe(speed = strafe)
            player.velocity.y = 0.005
        }
    }

    private fun handleVulcan288Packet(event: PacketEvent) {
        val packet = event.packet
        if (packet is PlayerMoveC2SPacket && player.velocity.y < 0) {
            packet.onGround = true
        }
    }

    private fun handleVulcanGround286Packet(event: PacketEvent) {
        if (event.packet is PlayerMoveC2SPacket && collidesBottomVertical() && !mc.options.jumpKey.isPressed) {
            event.packet.y += 0.005
        }
    }

    private fun collidesBottomVertical() =
        world.getBlockCollisions(player, player.boundingBox.offset(0.0, -0.005, 0.0)).any { shape ->
            shape != VoxelShapes.empty()
        }

    enum class VulcanMode(override val choiceName: String) : NamedChoice {
        VULCAN_GROUND_286("VulcanGround286"),
        VULCAN_288("Vulcan288"),
        VULCAN_286("Vulcan286")
    }
}

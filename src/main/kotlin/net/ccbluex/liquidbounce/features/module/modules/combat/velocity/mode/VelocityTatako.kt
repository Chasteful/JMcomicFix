package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

internal object VelocityTatako : VelocityMode("Tatako20250518Flag") {

    private var shouldCancelMovement = false
    private var wasSneaking = false

    override fun enable() {
        shouldCancelMovement = false
        wasSneaking = player.isSneaking
    }

    override fun disable() {
        if (!wasSneaking && player.isSneaking) {
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
        }
    }

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
            var sentSneak = false

            if (!player.isSneaking) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
                sentSneak = true
            }

            if (player.isSneaking || sentSneak) {
                // Use player's velocity setter method if available, or modify directly
                player.velocity.z = packet.velocityX / 8000.0
                player.velocity.y = packet.velocityY / 8000.0
                player.velocity.z = packet.velocityZ / 8000.0

                network.sendPacket(
                    PlayerMoveC2SPacket.PositionAndOnGround(
                        player.x,
                        player.y - 0.098,
                        player.z,
                        false,
                        true
                    )
                )

                shouldCancelMovement = true
                event.cancelEvent()
            }

            if (sentSneak) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
            }
        }
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (shouldCancelMovement) {
            shouldCancelMovement = false
        }
    }
}

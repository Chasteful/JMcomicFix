package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround

internal object NoFallAACv5 : Choice("AACv5") {
    private var isDmgFalling = false

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes


    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (player.fallDistance > 3.0f && isBlockUnder() && !isDmgFalling) {
            isDmgFalling = true
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket && isDmgFalling && packet.isOnGround && player.isOnGround) {
            isDmgFalling = false
            packet.onGround = true
            player.setOnGround(false)
            val newY = packet.y + 1.0
            network.sendPacket(
                PositionAndOnGround(
                    packet.x,
                    newY,
                    packet.z,
                    true,
                    false
                )
            )
            network.sendPacket(
                PositionAndOnGround(
                    packet.x,
                    newY - 1.0784,
                    packet.z,
                    false,
                    false
                )
            )
            network.sendPacket(
                PositionAndOnGround(
                    packet.x,
                    newY - 0.5,
                    packet.z,
                    true,
                    false
                )
            )
        }
    }
    private fun isBlockUnder(height: Double = 5.0): Boolean {
        val world = mc.world ?: return false
        val player = mc.player ?: return false
        var offset = 0.0
        while (offset < height) {
            val playerBox = player.boundingBox.offset(0.0, -offset, 0.0)
            if (world.getBlockCollisions(player, playerBox).iterator().hasNext()) {
                return true
            }
            offset += 0.5
        }
        return false
    }
}

package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround

internal object NoFallAACv5 : Choice("AACv5") {
    private var isDmgFalling = false

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket) {
            if (isDmgFalling && packet.isOnGround && player.isOnGround) {
                isDmgFalling = false
                packet.onGround = true
                player.setOnGround(false)

                // Send position adjustment packets
                network.sendPacket(
                    PositionAndOnGround(
                        packet.x,
                        packet.y + 1.0,
                        packet.z,
                        false,
                        false
                    )
                )
                network.sendPacket(
                    PositionAndOnGround(
                        packet.x,
                        packet.y - 1.0784,
                        packet.z,
                        false,
                        false
                    )
                )
                network.sendPacket(
                    PositionAndOnGround(
                        packet.x,
                        packet.y - 0.5,
                        packet.z,
                        false,
                        false
                    )
                )
            }

            // Detect when we should start damage falling prevention
            if (player.fallDistance > 2.5f && !isDmgFalling) {
                isDmgFalling = true
            }
        }
    }
}

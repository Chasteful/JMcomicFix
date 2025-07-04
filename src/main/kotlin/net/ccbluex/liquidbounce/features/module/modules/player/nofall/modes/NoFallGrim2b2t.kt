package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.ccbluex.liquidbounce.event.handler

internal object NoFallGrim2b2t : Choice("Grim2b2t") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is PlayerMoveC2SPacket && player.fallDistance > 0f) {

            network.sendPacket(
                PlayerMoveC2SPacket.PositionAndOnGround(
                    player.x,
                    player.y + 1E-9,
                    player.z,
                    true,
                    false
                )
            )
            player.fallDistance = 0f
            event.packet.onGround = true
        }
    }
}

package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.ccbluex.liquidbounce.event.handler

internal object NoFallAACv4 : Choice("AACv4") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when {

            event.packet is EntityVelocityUpdateS2CPacket && player.fallDistance > 1.8f -> {
                event.packet.velocityY = (event.packet.velocityY * -0.1).toInt()
            }

            event.packet is PlayerMoveC2SPacket && player.fallDistance > 1.6f -> {
                event.packet.onGround = true
            }
        }
    }
}

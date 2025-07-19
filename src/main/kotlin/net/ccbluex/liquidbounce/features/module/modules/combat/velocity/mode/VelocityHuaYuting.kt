package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket


internal object VelocityHuaYuting : VelocityMode("HuaYuting") {
    private var velocityInput = false
    private var canVelocity = false

    @Suppress("unused")
    private val tickHandler = tickHandler {
        canVelocity = player.hurtTime > 0
    }

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> {
        if (!velocityInput || !canVelocity) return@handler

        player.movement.x = 0.0
        player.movement.y = 0.0
        player.movement.z = 0.0

        velocityInput = false
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (event.packet is EntityVelocityUpdateS2CPacket && event.packet.entityId == player.id) {
            velocityInput = true
        }

        if (event.packet is PlayerPositionLookS2CPacket) {
            velocityInput = false
        }
    }

    override fun enable() {
        velocityInput = false
        canVelocity = false
    }

}

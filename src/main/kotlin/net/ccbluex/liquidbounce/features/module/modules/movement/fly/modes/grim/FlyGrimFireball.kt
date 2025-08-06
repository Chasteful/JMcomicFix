/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.grim

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.minecraft.entity.Entity
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import org.lwjgl.glfw.GLFW
import java.util.concurrent.LinkedBlockingQueue
import kotlin.Int

internal object FlyGrimFireball : Choice("GrimFireball ") {
    override val parent: ChoiceConfigurable<*>
        get() = modes

    private val velocityBeforeExplode by boolean("VelocityBeforeExplode", false)

    private val packets = LinkedBlockingQueue<Packet<*>>()
    private var target: Entity? = null
    private var s12count = 0

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is CommonPongC2SPacket || packet is KeepAliveC2SPacket) {
            event.cancelEvent()
            packets.add(packet)
        }

        if (packet is PlayerInteractEntityC2SPacket && target == null) {
            event.cancelEvent()
        }

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
            event.cancelEvent()
            packets.add(packet)
            s12count += 1
        }

        if (packet is ExplosionS2CPacket) {
            event.cancelEvent()
            packets.add(packet)
            s12count += 1
        }

        if (packet is BlockUpdateS2CPacket || packet is BlockEventS2CPacket || packet is BlockEntityUpdateS2CPacket) {
            event.cancelEvent()
            packets.add(packet)
        }

        if (packet is EntityS2CPacket && packet.getEntity(world) == mc.player) {
            event.cancelEvent()
            packets.add(packet)
        }

        if (packet is PlayerPositionLookS2CPacket) {
            packets.add(packet)
        }
    }

    @Suppress("unused")
    private val keyboardKeyEventHandler = handler<KeyboardKeyEvent> { event ->
        if (packets.isEmpty() || s12count <= 0 || event.key.code != GLFW.GLFW_KEY_K) return@handler

        var c0fId = -1
        var lastId = -1
        var packet = packets.take()

        // 提取复杂条件到单独的函数
        while (!shouldProcessPacket(packet)) {
            if (packet is CommonPongC2SPacket) {
                val newId: Int = packet.parameter

                if (c0fId != -1) {
                    if (c0fId - newId == 1) {
                        lastId = c0fId
                        c0fId = newId
                    } else if (lastId != -1 && lastId - newId == 1) {
                        c0fId = lastId
                    }
                } else {
                    c0fId = newId
                    lastId = -1
                }

                if (c0fId != -1 && lastId == -1) {
                    c0fId = newId
                }
            }
            sendPacketSilently(packet)
            packet = packets.take()
        }
        sendPacketSilently(packet)
        s12count -= 1

        while (!(packet is CommonPongC2SPacket && packet.parameter == c0fId - 1)) {
            packet = packets.take()
            sendPacketSilently(packet)
        }
        sendPacketSilently(packets.take())
    }

    private fun shouldProcessPacket(packet: Packet<*>): Boolean {
        return packet is EntityVelocityUpdateS2CPacket ||
            (packet is ExplosionS2CPacket &&
                (packet.playerKnockback.get().x != 0.0 ||
                    packet.playerKnockback.get().y != 0.0 ||
                    packet.playerKnockback.get().z != 0.0))
    }
    private fun blink() {
        val packets2 = LinkedBlockingQueue<EntityVelocityUpdateS2CPacket?>()
        val packets3 = LinkedBlockingQueue<ExplosionS2CPacket?>()
        val currentPackets = LinkedBlockingQueue<Packet<*>?>()
        while (!packets.isEmpty()) {
            val packet = packets.take()
            when (packet) {
                is EntityVelocityUpdateS2CPacket -> {
                    packets2.add(packet)
                }

                is ExplosionS2CPacket -> {
                    packets3.add(packet)
                }

                else -> {
                    currentPackets.add(packet)
                }
            }
        }
        if (velocityBeforeExplode) {
            while (!packets2.isEmpty()) {
                packets2.take()?.let { sendPacketSilently(it) }
            }
        }

        while (!packets3.isEmpty()) {
            packets3.take()?.let { sendPacketSilently(it) }
        }
        if (!velocityBeforeExplode) {
            while (!packets2.isEmpty()) {
                packets2.take()?.let { sendPacketSilently(it) }
            }
        }
        while (!currentPackets.isEmpty()) {
            currentPackets.take()?.let { sendPacketSilently(it) }
        }
        target = null
    }

    override fun enable() {
        s12count = 0
        target = if (ModuleKillAura.targetTracker.target != null) {
            ModuleKillAura.targetTracker.target
        } else {
            null
        }
        super.enable()
    }

    override fun disable() {
        blink()
        super.disable()
    }
}

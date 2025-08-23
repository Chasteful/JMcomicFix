package net.ccbluex.liquidbounce.features.module.modules.player.delayblink

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket

object DelayBlinkPacketManager : EventListener {

    var clear = false
    private var enabled = false
    private var ticks = 0
    private var full = false
    private val packets = mutableListOf<DelayPacket>()

    @Suppress("unused")
    private val tickPacketProcessEventHandler = handler<TickPacketProcessEvent> {
        if (!enabled) return@handler

        if (clear) {
            packets.removeIf {
                if (it.origin == TransferOrigin.OUTGOING) {
                    sendPacketSilently(it.packet)
                } else {
                    handlePacket(it.packet)
                }
                true
            }
            clear = false
            enabled = false
            notification(
                "DelayBlink",
                " Already Handled All Packets",
                NotificationEvent.Severity.INFO
            )
            return@handler
        }

        packets.removeIf {
            if (it.ticks + ModuleDelayBlink.delay < ticks) {
                if (it.origin == TransferOrigin.OUTGOING) {
                    sendPacketSilently(it.packet)
                } else {
                    handlePacket(it.packet)
                }
                true
            } else {
                false
            }
        }
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (!enabled) return@handler

        if (ModuleDelayBlink.autoDisable && event.packet is PlayerInteractEntityC2SPacket) {
            ModuleDelayBlink.enabled = false
        }

        if ((ModuleDelayBlink.DelayPacketTypes.OUTGOING in ModuleDelayBlink.delayPacketTypes
                && event.origin == TransferOrigin.OUTGOING)
            || (ModuleDelayBlink.DelayPacketTypes.INCOMING in ModuleDelayBlink.delayPacketTypes
                && event.origin == TransferOrigin.INCOMING)
        ) {
            event.cancelEvent()
            packets.add(DelayPacket(event.packet, ticks, event.origin))
        }
    }

    @Suppress("unused")
    private val checkEnabledHandler = tickHandler {
        waitUntil { ModuleDelayBlink.enabled }
        enabled = true
        packets.clear()
        ticks = 0
        full = false
        waitUntil { !enabled }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (!enabled) return@tickHandler

        ticks++
        if (ticks <= ModuleDelayBlink.delay) {
            if (ModuleDelayBlink.displayDelay) {
                notification(
                    "DelayBlink",
                    "Delay: $ticks / ${ModuleDelayBlink.delay} (Ticks)",
                    NotificationEvent.Severity.INFO
                )
            }
        } else if (!full) {
            notification(
                "DelayBlink",
                "Start Handling the Packets ${ModuleDelayBlink.delay} Ticks Ago...",
                NotificationEvent.Severity.INFO
            )
            full = true
        }
    }

    private data class DelayPacket(val packet: Packet<*>, val ticks: Int, val origin: TransferOrigin)

}

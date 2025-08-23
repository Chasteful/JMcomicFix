package net.ccbluex.liquidbounce.features.module.modules.player.fireballfly

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.fireballfly.ModuleFireballFly.packetProcessQueue
import net.ccbluex.liquidbounce.features.module.modules.player.fireballfly.ModuleFireballFly.processPackets
import net.ccbluex.liquidbounce.utils.client.handlePacket

object FireballFlyPacketManager : EventListener {

    @Suppress("unused")
    private val handleTickPacketProcess = handler<TickPacketProcessEvent> {
        processPackets()

        packetProcessQueue.removeIf {
            handlePacket(it)
            true
        }
    }

}

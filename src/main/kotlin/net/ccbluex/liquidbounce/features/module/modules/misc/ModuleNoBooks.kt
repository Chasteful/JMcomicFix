package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket


object ModuleNoBooks : ClientModule("NoBooks", ModuleCategories.MISC) {

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(
        priority = EventPriorityConvention.SAFETY_FEATURE
    ) { event ->
        if (event.packet is ClientboundOpenBookPacket) {
            event.cancelEvent()

            mc.execute {
                if (player.containerMenu.containerId == event.packet.hand.ordinal) {
                    player.closeContainer()
                }
            }
        }
    }
}

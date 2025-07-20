package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.packet.s2c.play.OpenWrittenBookS2CPacket

/**
 * 自动关闭打开的书本界面
 */
object ModuleNoBooks : ClientModule("NoBooks", Category.MISC) {

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(
        priority = EventPriorityConvention.SAFETY_FEATURE
    ) { event ->

        if (event.packet is OpenWrittenBookS2CPacket) {
            event.cancelEvent()

            if (player.currentScreenHandler.syncId == event.packet.hand.ordinal) {
                player.closeHandledScreen()
            }
        }
    }
}

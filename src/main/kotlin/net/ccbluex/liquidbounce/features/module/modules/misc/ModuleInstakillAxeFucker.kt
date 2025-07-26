package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.inventory.*
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.Items
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType

object ModuleInstakillAxeFucker : ClientModule("InstakillAxeFucker", Category.PLAYER) {

    private val inventoryConstraints = tree(InventoryConstraints())
    private var paused = false
    private var hasPerformedActions = false

    private fun Slot.toContainerItemSlot(): ContainerItemSlot = ContainerItemSlot(this.id)

    @Suppress("unused")
    private val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        if (paused) return@handler

        val paperSlot = Slots.OffhandWithHotbar.findSlot(Items.MAP)?.hotbarSlotForServer
        if (paperSlot != null && player.inventory.main.any { it.item == Items.BOW }) {
            SilentHotbar.selectSlotSilently(this,paperSlot, 0)
        } else {
            return@handler
        }

        KeyBinding.setKeyPressed(mc.options.useKey.boundKey, true)

        val screen = mc.currentScreen as? GenericContainerScreen ?: return@handler

        val chestSlot = screen.screenHandler.slots.firstOrNull { it.stack.item == Items.CHEST }
        if (chestSlot != null) {
            event.schedule(
                inventoryConstraints,
                ClickInventoryAction.click(screen, chestSlot.toContainerItemSlot(), 0, SlotActionType.PICKUP)
            )
        }

        val helmetSlot = screen.screenHandler.slots.firstOrNull { it.stack.item == Items.LEATHER_HELMET }
        if (helmetSlot != null) {
            event.schedule(
                inventoryConstraints,
                ClickInventoryAction.click(screen, helmetSlot.toContainerItemSlot(), 0, SlotActionType.PICKUP)
            )
            if (hasPerformedActions) {
                KeyBinding.setKeyPressed(mc.options.useKey.boundKey, false)
                mc.setScreen(null)
                notification(
                    "InstakillAxeFucker", "InstakillAxe has been auto selected with no probability.",
                    NotificationEvent.Severity.INFO
                )
                paused = true
            }
        }
    }

    @Suppress("unused")
    private val chatMonitor = handler<ChatReceiveEvent> { event ->
        val localName = player.name.string
        val message = event.textData.string

        if (message.startsWith(localName) && message.contains("已经为", ignoreCase = true)) {
            hasPerformedActions = true
        }
    }

    @Suppress("unused")
    private val respawnListener = handler<PacketEvent> { event ->
        if (event.packet is PlayerRespawnS2CPacket) {
            paused = false
            hasPerformedActions = false
        }
    }

    @Suppress("unused")
    private val worldChangeListener = handler<WorldChangeEvent> {
        paused = false
        hasPerformedActions = false
    }
}

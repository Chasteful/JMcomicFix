package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClickGuiValueChangeEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

object ModulePendant : ClientModule("Pendant", Category.FUN, aliases = arrayOf("Wife")) {
    @Suppress("unused")
    private val custom by text ("CustomURL","")
        .onChanged {
            EventManager.callEvent(ClickGuiValueChangeEvent(this))
        }
    @Suppress("unused")
    private val scale by float("Scale", 1f, 0.5f..2f)
        .onChanged {
            EventManager.callEvent(ClickGuiValueChangeEvent(this))
    }
    @Suppress("unused")
    private val Alpha by int("Alpha",255,0..255)
        .onChanged {
            EventManager.callEvent(ClickGuiValueChangeEvent(this))
        }
}

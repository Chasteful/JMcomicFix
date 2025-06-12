package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.item.consume.UseAction


object ModuleProgress : ClientModule("Progress", category = Category.RENDER) {

    private val weight by float("Weight", 1f, 0.1f..2f)
    private val height by float("Height", 1f, 0.1f..2f)

    private var eatingStartTime: Long = 0
    private var isEating: Boolean = false
    private var eatingMaxDuration: Int = 32

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val usingItem = player.isUsingItem
        val useAction = player.activeItem.useAction

        if (usingItem && (useAction == UseAction.EAT || useAction == UseAction.DRINK)) {
            if (!isEating) {
                isEating = true
                eatingStartTime = System.currentTimeMillis()
                eatingMaxDuration = player.activeItem.getMaxUseTime(player)
            }
        } else {
            isEating = false
        }
    }

    @Suppress("unused")
    private val renderProgressBar = handler<OverlayRenderEvent> {
        if (!isEating) return@handler

        val ctx = it.context
        val sw = ctx.scaledWindowWidth
        val sh = ctx.scaledWindowHeight

        val elapsed = (System.currentTimeMillis() - eatingStartTime).coerceAtLeast(0)
        val progress = (elapsed / (eatingMaxDuration * 50f)).coerceIn(0f..1f)

        val barWidth = (sw * 0.45f * weight).toInt()
        val barHeight = (8 * height).toInt()
        val barX = (sw - barWidth) / 2
        val barY = (sh / 2 + 40)


        ctx.fill(barX, barY, barX + barWidth, barY + barHeight, 0x88000000.toInt())

        val filledWidth = (barWidth * progress).toInt()

        ctx.fillGradient(
            barX,
            barY,
            barX + filledWidth,
            barY + barHeight,
            0,
            ModuleHud.PrimaryColor.toARGB(),
            ModuleHud.SecondaryColor.toARGB()
        )
    }
}

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.consume.UseAction

object ModuleProgress : ClientModule("Progress", category = Category.RENDER) {

    private val weight by float("Weight", 1f, 0.1f..2f)
    private val height by float("Height", 1f, 0.1f..2f)

    private var eatingStartTime: Long = 0
    private var isEating: Boolean = false
    private var eatingMaxDuration: Int = 32

    data class ProgressBarConfig(
        val ctx: DrawContext,
        val screenWidth: Int,
        val yPos: Int,
        val progress: Float,
        val weight: Float = 1f,
        val height: Float = 1f,
        val colorStart: Int,
        val colorEnd: Int
    )

    fun renderProgressBar(config: ProgressBarConfig) {
        val barWidth = (config.screenWidth * 0.45f * config.weight).toInt()
        val barHeight = (8 * config.height).toInt()
        val barX = (config.screenWidth - barWidth) / 2

        config.ctx.fill(barX, config.yPos, barX + barWidth, config.yPos + barHeight, 0x88000000.toInt())

        val filledWidth = (barWidth * config.progress).toInt()
        config.ctx.fillGradient(
            barX,
            config.yPos,
            barX + filledWidth,
            config.yPos + barHeight,
            0,
            config.colorStart,
            config.colorEnd
        )
    }

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
    private val renderHandler = handler<OverlayRenderEvent> {
        if (!isEating) return@handler

        val ctx = it.context
        val sw = ctx.scaledWindowWidth
        val sh = ctx.scaledWindowHeight

        val elapsed = (System.currentTimeMillis() - eatingStartTime).coerceAtLeast(0)
        val progress = (elapsed / (eatingMaxDuration * 50f)).coerceIn(0f..1f)
        renderProgressBar(ProgressBarConfig(
            ctx = ctx,
            screenWidth = sw,
            yPos = sh / 2 + 40,
            progress = progress,
            weight = weight,
            height = height,
            colorStart = ModuleHud.PrimaryColor.toARGB(),
            colorEnd = ModuleHud.SecondaryColor.toARGB()
        ))
    }
}

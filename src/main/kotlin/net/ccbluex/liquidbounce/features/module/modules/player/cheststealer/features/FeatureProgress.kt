package net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleProgress.ProgressBarConfig
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleProgress.renderProgressBar
import kotlin.math.pow


object FeatureProgress : ToggleableConfigurable(ModuleChestStealer, "Progress", true) {

    private val weight by float("Weight", 1f, 0.1f..2f)
    private val height by float("Height", 1f, 0.1f..2f)

    private val smooth by boolean( "Smooth", true)

    private var lastProgress = 0f
    private var lastUpdateTime = System.nanoTime()


    @JvmStatic
    var stealingStartTime: Long = 0L
    @JvmStatic
    var initialItemCount: Int = 0  // 初始物品总数
    @JvmStatic
    var remainingItems: Int = 0   // 当前剩余物品数

    fun onStartStealing(total: Int) {

        stealingStartTime = System.currentTimeMillis()
        initialItemCount = total
        remainingItems = total
    }

    fun updateRemainingItems(count: Int) {
        remainingItems = count
    }
    fun getStealingProgress(): Float {
        val target = if (initialItemCount <= 0) {
            0f
        } else {
            val stolenItems = (initialItemCount - remainingItems).coerceAtLeast(0)
            (stolenItems.toFloat() / initialItemCount).coerceIn(0f..1f)
        }

        if (!smooth) {
            lastProgress = target
            return target
        }

        val now = System.nanoTime()
        val deltaSec = (now - lastUpdateTime) / 1_000_000_000f
        lastUpdateTime = now

        val factor = 1f - 0.1.pow(deltaSec.toDouble()).toFloat()
        lastProgress += (target - lastProgress) * factor
        return lastProgress
    }

    @Suppress("unused")
    private val renderChestStealBar = handler<OverlayRenderEvent> {
        fun breakRequirement() = (
                !running || initialItemCount == 0
            )

        if (breakRequirement()) {
            return@handler
        }
        val ctx = it.context
        val sw = ctx.scaledWindowWidth
        val sh = ctx.scaledWindowHeight

        renderProgressBar(ProgressBarConfig(
            ctx = ctx,
            screenWidth = sw,
            yPos = sh / 2 + 40,
            progress = getStealingProgress(),
            weight = weight,
            height = height,
            colorStart = ModuleHud.PrimaryColor.toARGB(),
            colorEnd = ModuleHud.SecondaryColor.toARGB()
        ))
    }
}

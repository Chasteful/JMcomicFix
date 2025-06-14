package net.ccbluex.liquidbounce.features.module.modules.render.jumpcircle

import it.unimi.dsi.fastutil.objects.ObjectLongMutablePair
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.shiftHue
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.util.math.Vec3d

object ModuleJumpEffect : ClientModule("JumpEffect", Category.RENDER) {

    private val endRadius by floatRange("EndRadius", 0.15F..0.8F, 0F..3F)

    private val innerColor by color("InnerColor", Color4b(0, 255, 4, 0))
    private val outerColor by color("OuterColor", Color4b(0, 255, 4, 89))

    private val animCurve by curve("AnimCurve", Easing.QUAD_OUT)

    private val hueOffsetAnim by int("HueOffsetAnim", 63, -360..360)

    private val lifetime by int("Lifetime", 15, 1..30)

    private val circles = ArrayDeque<ObjectLongMutablePair<Vec3d>>()

    val repeatable = tickHandler {
        with(circles.iterator()) {
            while (hasNext()) {
                val pair = next()
                val newValue = pair.valueLong() + 1L
                if (newValue >= lifetime) {
                    remove()
                    continue
                }
                pair.value(newValue)
            }
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderEnvironmentForWorld(matrixStack) {
            circles.forEach {
                val progress = animCurve
                    .transform((it.valueLong() + event.partialTicks) / lifetime)
                    .coerceIn(0f..1f)

                withPositionRelativeToCamera(it.key()) {
                    drawGradientCircle(
                        endRadius.endInclusive * progress,
                        endRadius.start * progress,
                        animateColor(outerColor, progress),
                        animateColor(innerColor, progress)
                    )
                }
            }
        }

    }


    private fun animateColor(baseColor: Color4b, progress: Float): Color4b {
        val color = baseColor.fade(1.0F - progress)

        if (hueOffsetAnim == 0){
            return color
        }

        return shiftHue(color, (hueOffsetAnim * progress).toInt())
    }

    @Suppress("unused")
    val onJump = handler<PlayerJumpEvent> { _ ->
        // Adds new circle when the player jumps
        circles.add(ObjectLongMutablePair(player.pos, 0L))
    }

}

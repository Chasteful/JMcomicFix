package net.ccbluex.liquidbounce.features.module.modules.render.targetinfo.mode

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.render.targetinfo.ModuleTargetInfo
import net.ccbluex.liquidbounce.features.module.modules.render.targetinfo.ModuleTargetInfo.colorModes
import net.ccbluex.liquidbounce.features.module.modules.render.targetinfo.TargetInfoMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Identifier
import java.util.function.Function

object NovolineMode : TargetInfoMode("Novoline") {

    private var easingHealth = 0f
    private var alpha = 0
    private var delayCounter = 0
    private var previousEasingHealth = 0f
    private var lastTarget: LivingEntity? = null

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> {
        val target = determineTarget()
        renderTargetHUD(it.context, target)
    }

    private fun determineTarget(): LivingEntity? {
        val kaTarget = ModuleKillAura.targetTracker.target?.takeIf { it is PlayerEntity }
        return when {
            kaTarget != null -> kaTarget.also {
                delayCounter = 0
                lastTarget = it
            }
            delayCounter < 10 && lastTarget != null -> lastTarget.also { delayCounter++ }
            else -> null.also {
                delayCounter = 0
                lastTarget = null
            }
        }
    }

    private fun renderTargetHUD(ctx: DrawContext, target: LivingEntity?) {
        if (target != null || (lastTarget != null && delayCounter < 10)) {
            updateAnimationStates(target ?: lastTarget!!)
        } else {
            alpha = 0
            lastTarget = null
            easingHealth = 0f
            return
        }

        if (alpha <= 0) return

        val entity = lastTarget as? PlayerEntity ?: return
        val mc = net.minecraft.client.MinecraftClient.getInstance()

        val nameWidth = mc.textRenderer.getWidth(entity.name.string) * 0.3f
        val width = 36f + 6f + nameWidth + 64f
        val scaledWidth = mc.window.scaledWidth
        val scaledHeight = mc.window.scaledHeight

        val x = scaledWidth * ModuleTargetInfo.xOffsetRatio
        val y = scaledHeight * ModuleTargetInfo.yOffsetRatio

        val bgColor = ModuleTargetInfo.backgroundColor.withAlpha(
            (ModuleTargetInfo.backgroundColor.a * alpha / 255f).toInt())
        val bdColor = ModuleTargetInfo.borderColor.withAlpha((
            ModuleTargetInfo.borderColor.a * alpha / 255f).toInt())
        val txtColor = ModuleTargetInfo.textColor.withAlpha((
            ModuleTargetInfo.textColor.a * alpha / 255f).toInt())

        ctx.fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + 36).toInt(), bgColor.toARGB())


        ctx.fill((x - 1).toInt(), (y - 1).toInt(), (x + width + 1).toInt(), y.toInt(), bdColor.toARGB()) // 上
        ctx.fill((x - 1).toInt(), (y + 36).toInt(), (x + width + 1).toInt(), (y + 37).toInt(), bdColor.toARGB()) // 下
        ctx.fill((x - 1).toInt(), y.toInt(), x.toInt(), (y + 36).toInt(), bdColor.toARGB()) // 左
        ctx.fill((x + width).toInt(), y.toInt(), (x + width + 1).toInt(), (y + 36).toInt(), bdColor.toARGB()) // 右

        drawHealthBar(ctx, entity, width, x, y)
        drawPlayerHead(ctx, x, y)
        drawText(ctx, entity, txtColor, width, x, y)
    }

    private fun updateAnimationStates(entity: LivingEntity) {
        val mc = net.minecraft.client.MinecraftClient.getInstance()
        val health = entity.getActualHealth(true)
        val max = (entity.maxHealth + entity.absorptionAmount).coerceAtLeast(1f)
        val delta = mc.renderTickCounter.getTickDelta(true)

        if (max <= 0f) return

        val smoothing = 0.2f
        val fadeSmoothing = 0.1f
        easingHealth += (health - easingHealth) * smoothing * delta
        easingHealth = easingHealth.coerceIn(0f, max)


        previousEasingHealth += (easingHealth - previousEasingHealth) * fadeSmoothing * delta
        previousEasingHealth = previousEasingHealth.coerceIn(0f, max)

        val targetAlpha = if (ModuleKillAura.targetTracker.target != null || delayCounter < 10) 255 else 0
        alpha = (alpha + (targetAlpha - alpha) * smoothing * delta).toInt().coerceIn(0, 255)
    }

    @Suppress("LongParameterList")
    private fun drawText(ctx: DrawContext, target: PlayerEntity, color: Color4b, width: Float, x: Float, y: Float) {
        val mc = net.minecraft.client.MinecraftClient.getInstance()
        val max = (target.maxHealth + target.absorptionAmount).coerceAtLeast(1f)
        val percent = (easingHealth / max * 100).toInt().coerceIn(0, 100).toString() + "%"
        val percentWidth = mc.textRenderer.getWidth(percent) * 0.3f
        val percentX = x + 38f + ((width - 40f) - percentWidth) / 2f
        val percentY = y + 24f + 4f - (mc.textRenderer.fontHeight * 0.3f / 2f) - 1f

        val name = mc.textRenderer.trimToWidth(target.name.string, ((width - 44f) / 0.3f).toInt())

        ctx.drawText(mc.textRenderer, name, (x + 38f).toInt(), (y + 6f).toInt(), color.toARGB(), false)
        ctx.drawText(mc.textRenderer, percent, percentX.toInt(), percentY.toInt(), Color4b.WHITE.toARGB(), false)
    }


    private fun drawHealthBar(ctx: DrawContext, target: LivingEntity, width: Float, x: Float, y: Float) {
        val barX = x + 38f
        val barY = y + 24f
        val barW = (width - 40f).coerceAtLeast(0f)

        val maxHealth = (target.maxHealth + target.absorptionAmount).coerceAtLeast(1f)
        val currentHealth = (easingHealth / maxHealth).coerceIn(0f, 1f) * barW
        val previousHealth = (previousEasingHealth / maxHealth).coerceIn(0f, 1f) * barW

        val (start, end) = when (val mode = colorModes.activeChoice) {
            is GenericStaticColorMode, is GenericRainbowColorMode -> mode.getColors(mc.player).first.let { it to it }
            else -> mode.getColors(mc.player)
        }

        ctx.fill(
            barX.toInt(), barY.toInt(), (barX + barW).toInt(), (barY + 8f).toInt(),
            Color4b(40, 40, 40, (40 * alpha / 255f).toInt()).toARGB()
        )

        if (previousHealth != currentHealth) {
            val fadeStartX = barX + minOf(previousHealth, currentHealth)
            val fadeWidth = (previousHealth - currentHealth).coerceIn(-barW, barW)
            ctx.fillGradient(
                fadeStartX.toInt(), barY.toInt(), (fadeStartX + fadeWidth).toInt(), (barY + 8f).toInt(), 0,
                start.with(a = (start.a * alpha / 255f * 0.5f).toInt()).toARGB(),
                end.with(a = (end.a * alpha / 255f * 0.5f).toInt()).toARGB()
            )
        }


        ctx.fillGradient(
            barX.toInt(), barY.toInt(), (barX + currentHealth).toInt(), (barY + 8f).toInt(), 0,
            start.with(a = (start.a * alpha / 255f).toInt()).toARGB(),
            end.with(a = (end.a * alpha / 255f).toInt()).toARGB()
        )
    }

    private fun drawPlayerHead(ctx: DrawContext, x: Float, y: Float) {
        val target = lastTarget as? PlayerEntity ?: return
        val mc = net.minecraft.client.MinecraftClient.getInstance()
        val id = mc.skinProvider.getSkinTextures(target.gameProfile).texture()
        val centerX = x + 8f
        val centerY = y + 8f
        val alphaMask = (alpha.coerceIn(0, 255) shl 24) or 0xFFFFFF

        ctx.matrices.push()
        ctx.matrices.translate(centerX + 1, centerY + 1, 0f)
        ctx.matrices.scale(4.5f, 4.5f, 1f)
        ctx.matrices.translate(-centerX, -centerY, 0f)

        val layer = Function<Identifier, RenderLayer> { RenderLayer.getGuiTextured(it) }

        ctx.drawTexture(layer, id, (x + 6).toInt(), (y + 6).toInt(), 8f, 8f, 8, 8, 64, 64, alphaMask)
        ctx.drawTexture(layer, id, (x + 6).toInt(), (y + 6).toInt(), 40f, 8f, 8, 8, 64, 64, alphaMask)

        ctx.matrices.pop()
    }
}

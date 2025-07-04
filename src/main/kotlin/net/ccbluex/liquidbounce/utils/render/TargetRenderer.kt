/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.lastRenderPos
import net.ccbluex.liquidbounce.utils.math.interpolate
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.render.WorldToScreen.calculateScreenPos
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import org.joml.Quaternionf
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin


/**
 * A target tracker to choose the best enemy to attack
 */
sealed class TargetRenderer<T: RenderEnvironment>(
    module: ClientModule
) : ToggleableConfigurable(module, "TargetRendering", true) {

    init {
        doNotIncludeAlways()
    }

    abstract val appearance: ChoiceConfigurable<out TargetRenderAppearance<in T>>

    open fun render(env: T, entity: Entity, partialTicks: Float) {
        if (!enabled) {
            return
        }

        appearance.activeChoice.render(env, entity, partialTicks)
    }

}

class WorldTargetRenderer(module: ClientModule) : TargetRenderer<WorldRenderEnvironment>(module) {

    override val appearance = choices(module, "Mode", 2) {
        arrayOf(Circle(module),Capture() ,Ghost())
    }

    inner class Ghost : WorldTargetRenderAppearance("Ghost") {

        private val glow = "particles/glow.png".registerAsDynamicImageFromClientResources()

        private var lastTime = System.currentTimeMillis()

        override val parent: ChoiceConfigurable<*>
            get() = appearance

        private val color by color("Color", Color4b(Color.BLUE.rgb, true))
        private var size by float("Size", 0.5f, 0.4f..0.7f)
        private var length by int("Length", 25, 15..40)

        override fun render(env: WorldRenderEnvironment, entity: Entity, partialTicks: Float) {
            env.matrixStack.push()
            RenderSystem.depthMask(false)
            RenderSystem.disableCull()
            mc.gameRenderer.lightmapTextureManager.disable()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ZERO,
                GlStateManager.DstFactor.ONE
            )

            with(mc.gameRenderer.camera.pos) {
                env.matrixStack.translate(-this.x, -this.y, -this.z)
            }

            val interpolated = entity.pos.interpolate(entity.lastRenderPos(), partialTicks.toDouble())
                .add(0.0, 0.75, 0.0)

            with(interpolated) {
                env.matrixStack.translate(
                    this.x + 0.2f,
                    this.y + 0.5f,
                    this.z
                )
            }

            RenderSystem.setShaderTexture(0, glow)

            with(env) {
                drawParticle(
                    { sin, cos -> Vec3d(sin, cos, -cos) },
                    { sin, cos -> Vec3d(-sin, -cos, cos) }
                )

                drawParticle(
                    { sin, cos -> Vec3d(-sin, sin, -cos) },
                    { sin, cos -> Vec3d(sin, -sin, cos) }
                )

                drawParticle(
                    { sin, cos -> Vec3d(-sin, -sin, cos) },
                    { sin, cos -> Vec3d(sin, sin, -cos) }
                )
            }

            RenderSystem.depthMask(true)
            RenderSystem.defaultBlendFunc()
            mc.gameRenderer.lightmapTextureManager.enable()
            RenderSystem.enableCull()
            env.matrixStack.pop()
        }

        private inline fun WorldRenderEnvironment.drawParticle(
            translationsBefore: MatrixStack.(Double, Double) -> Vec3d,
            translateAfter: MatrixStack.(Double, Double) -> Vec3d
        ) {
            val radius = 0.67
            val distance = 10.0 + (length * 0.2)
            val alphaFactor = 15

            for (i in 0..<length) {
                val angle: Double = 0.15f * (System.currentTimeMillis() - lastTime - (i * distance)) / (30)
                val sin = sin(angle) * radius
                val cos = cos(angle) * radius

                with(matrixStack) {
                    with(translationsBefore(sin, cos)) {
                        translate(x, y, z)
                    }

                    translate(-size / 2.0, -size / 2.0, 0.0)
                    multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.camera.yaw))
                    multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.camera.pitch))
                    translate(size / 2.0, size / 2.0, 0.0)
                }

                val alpha = MathHelper.clamp(color.a - (i * alphaFactor), 0, color.a)
                val renderColor = color.alpha(alpha)

                drawCustomMesh(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR,
                    ShaderProgramKeys.POSITION_TEX_COLOR
                ) { matrix ->
                    vertex(matrix, 0.0f, -size, 0.0f)
                        .texture(0.0f, 0.0f)
                        .color(renderColor.toARGB())

                    vertex(matrix, -size, -size, 0.0f)
                        .texture(0.0f, 1.0f)
                        .color(renderColor.toARGB())

                    vertex(matrix, -size, 0.0f, 0.0f)
                        .texture(1.0f, 1.0f)
                        .color(renderColor.toARGB())

                    vertex(matrix, 0.0f, 0.0f, 0.0f)
                        .texture(1.0f, 0.0f)
                        .color(renderColor.toARGB())
                }

                with(matrixStack) {
                    translate(-size / 2.0, -size / 2.0, 0.0)
                    multiply(RotationAxis.POSITIVE_X.rotationDegrees(-mc.gameRenderer.camera.pitch))
                    multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mc.gameRenderer.camera.yaw))
                    translate(size / 2.0, size / 2.0, 0.0)

                    with(translateAfter(sin, cos)) {
                        translate(x, y, z)
                    }
                }
            }
        }
    }

    inner class Capture : WorldTargetRenderAppearance("Capture") {
        override val parent: ChoiceConfigurable<*>
            get() = appearance

        private val captureTexture = "particles/capture.png"
            .registerAsDynamicImageFromClientResources()
        private val color by color("Color", Color4b(Color.RED.rgb, true))
        private val size by float("Size", 0.5f, 0.1f..2f)
        private val rotationSpeed by float("RotationSpeed", 1f, 0.1f..5f)
        private val reverse by boolean("Reverse", false)
        private val fadeIn by float("FadeIn", 0.2f, 0f..1f)
        private val fadeOut by float("FadeOut", 0.4f, 0f..1f)

        override fun render(env: WorldRenderEnvironment, entity: Entity, partialTicks: Float) {
            env.matrixStack.push()
            RenderSystem.depthMask(false)
            RenderSystem.disableCull()
            mc.gameRenderer.lightmapTextureManager.disable()
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ONE
            )

            with(mc.gameRenderer.camera.pos) {
                env.matrixStack.translate(-this.x, -this.y, -this.z)
            }

            val interpolated = entity.pos
                .interpolate(entity.lastRenderPos(), partialTicks.toDouble())
                .add(0.0, entity.height / 2.0, 0.0)

            with(interpolated) {
                env.matrixStack.translate(this.x, this.y, this.z)
            }

            RenderSystem.setShaderTexture(0, captureTexture)

            val time = System.currentTimeMillis() / 50f // 更敏感的时间计算
            val rotationAngle = time * rotationSpeed * if (reverse) -1 else 1
            val cycleTime = time % (fadeIn + fadeOut)
            val alpha = when {
                cycleTime < fadeIn -> cycleTime / fadeIn
                else -> 1f - (cycleTime - fadeIn) / fadeOut
            }.coerceIn(0f, 1f)
            val renderColor = color.alpha((alpha * color.a).toInt())

            env.matrixStack.apply {
                multiply(mc.gameRenderer.camera.rotation)
                multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationAngle)) // 使用RotationAxis
                scale(size, size, size)
                translate(-0.5f, -0.5f, 0.0f)
            }

            env.drawCustomMesh(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR,
                ShaderProgramKeys.POSITION_TEX_COLOR
            ) { matrix ->
                vertex(matrix, 0f, 0f, 0f).texture(0f, 1f).color(renderColor.toARGB())
                vertex(matrix, 1f, 0f, 0f).texture(1f, 1f).color(renderColor.toARGB())
                vertex(matrix, 1f, 1f, 0f).texture(1f, 0f).color(renderColor.toARGB())
                vertex(matrix, 0f, 1f, 0f).texture(0f, 0f).color(renderColor.toARGB())
            }

            env.matrixStack.pop()

            RenderSystem.depthMask(true)
            RenderSystem.defaultBlendFunc()
            mc.gameRenderer.lightmapTextureManager.enable()
            RenderSystem.enableCull()
            env.matrixStack.pop()
        }
    }

    inner class Circle(module: ClientModule) : WorldTargetRenderAppearance("GlowingCircle") {
        override val parent: ChoiceConfigurable<*>
            get() = appearance

        private val radius by float("Radius", 0.85f, 0.1f..2f)

        private val heightMode = choices(module, "HeightMode") {
            arrayOf(FeetHeight(it), TopHeight(it), RelativeHeight(it), HealthHeight(it), AnimatedHeight(it))
        }

        private val color by color("OuterColor", Color4b(0x64007CFF, true))
        private val glowColor by color("GlowColor", Color4b(0x00007CFF, true))

        private val glowHeightSetting by float("GlowHeight", 0.3f, -1f..1f)

        private val outline = tree(Outline())

        override fun render(env: WorldRenderEnvironment, entity: Entity, partialTicks: Float) {
            val height = heightMode.activeChoice.getHeight(entity, partialTicks)
            val pos = entity.interpolateCurrentPosition(partialTicks) + Vec3d(0.0, height, 0.0)

            val currentHeightMode = heightMode.activeChoice

            val glowHeight = if (currentHeightMode is HeightWithGlow) {
                currentHeightMode.getGlowHeight(entity, partialTicks) - height
            } else {
                glowHeightSetting.toDouble()
            }

            with(env) {
                withPosition(this.relativeToCamera(pos)) {
                    withDisabledCull {
                        drawGradientCircle(
                            radius,
                            radius,
                            color,
                            glowColor,
                            Vec3(0.0, glowHeight, 0.0)
                        )

                        drawGradientCircle(
                            radius,
                            0f,
                            color,
                            color
                        )
                    }
                    if (outline.enabled) {
                        drawCircleOutline(radius, outline.color)
                    }
                }
            }
        }

    }

    inner class Outline : ToggleableConfigurable(parent, "Outline", true) {
        val color by color("Color", Color4b(0x00007CFF, false))
    }

    inner class FeetHeight(private val choiceConfigurable: ChoiceConfigurable<*>) : HeightMode("Feet") {
        override val parent: ChoiceConfigurable<*>
            get() = choiceConfigurable

        val offset by float("Offset", 0f, -1f..1f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            return offset.toDouble()
        }

    }

    inner class TopHeight(private val choiceConfigurable: ChoiceConfigurable<*>) : HeightMode("Top") {
        override val parent: ChoiceConfigurable<*>
            get() = choiceConfigurable

        val offset by float("Offset", 0f, -1f..1f)
        override fun getHeight(entity: Entity, partialTicks: Float) = entity.box.maxY - entity.box.minY + offset
    }

    // Lets the user chose the height relative to the entity's height
    // Use 1 for it to always be at the top of the entity
    // Use 0 for it to always be at the feet of the entity

    inner class RelativeHeight(private val choiceConfigurable: ChoiceConfigurable<*>) : HeightMode("Relative") {
        override val parent: ChoiceConfigurable<*>
            get() = choiceConfigurable

        private val height by float("Height", 0.5f, -0.5f..1.5f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            val box = entity.box
            val entityHeight = box.maxY - box.minY
            return height * entityHeight
        }
    }

    inner class HealthHeight(private val choiceConfigurable: ChoiceConfigurable<*>) : HeightMode("Health") {
        override val parent: ChoiceConfigurable<*>
            get() = choiceConfigurable

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            if (entity !is LivingEntity) return 0.0
            val box = entity.box
            val entityHeight = box.maxY - box.minY
            return entity.health / entity.maxHealth * entityHeight
        }
    }

    inner class AnimatedHeight(private val choiceConfigurable: ChoiceConfigurable<*>) : HeightWithGlow("Animated") {
        override val parent: ChoiceConfigurable<*>
            get() = choiceConfigurable

        private val speed by float("Speed", 0.18f, 0.01f..1f)
        private val heightMultiplier by float("HeightMultiplier", 0.4f, 0.1f..1f)
        private val heightOffset by float("HeightOffset", 1.3f, 0f..2f)
        private val glowOffset by float("GlowOffset", -1f, -3.1f..3.1f)

        override fun getHeight(entity: Entity, partialTicks: Float): Double {
            return calculateHeight((entity.age + partialTicks) * speed)
        }

        override fun getGlowHeight(entity: Entity, partialTicks: Float): Double {
            return calculateHeight((entity.age + partialTicks) * speed + glowOffset)
        }

        private fun calculateHeight(time: Float) =
            (sin(time) * heightMultiplier + heightOffset).toDouble()
    }
}

class OverlayTargetRenderer(module: ClientModule) : TargetRenderer<GUIRenderEnvironment>(module) {
override val appearance = choices<TargetRenderAppearance<GUIRenderEnvironment>>(module, "Mode") {
    arrayOf(Legacy())
}

inner class Legacy : OverlayTargetRenderAppearance("Arrow") {

    override val parent: ChoiceConfigurable<TargetRenderAppearance<GUIRenderEnvironment>>
        get() = appearance

    private val color by color("Color", Color4b.RED)
    private val size by float("Size", 1.5f, 0.5f..20f)

    override fun render(env: GUIRenderEnvironment, entity: Entity, partialTicks: Float) {
        val pos =
            entity.interpolateCurrentPosition(partialTicks) +
                Vec3d(0.0, entity.height.toDouble(), 0.0)

        val screenPos = calculateScreenPos(pos) ?: return

        with(env) {
            withColor(color) {
                drawCustomMesh(
                    VertexFormat.DrawMode.TRIANGLE_STRIP,
                    VertexFormats.POSITION,
                    ShaderProgramKeys.POSITION
                ) {
                    vertex(it, screenPos.x - 5 * size, screenPos.y - 10 * size, 1f)
                    vertex(it, screenPos.x, screenPos.y, 1f)
                    vertex(it, screenPos.x + 5 * size, screenPos.y - 10 * size, 1f)
                }
            }
        }
    }
}
}

sealed class TargetRenderAppearance<T: RenderEnvironment>(name: String) : Choice(name) {
    open fun render(env: T, entity: Entity, partialTicks: Float) {}
}

sealed class WorldTargetRenderAppearance(name: String) : TargetRenderAppearance<WorldRenderEnvironment>(name)
sealed class OverlayTargetRenderAppearance(name: String) : TargetRenderAppearance<GUIRenderEnvironment>(name)

sealed class HeightMode(name: String) : Choice(name) {
    open fun getHeight(entity: Entity, partialTicks: Float): Double = 0.0
}

sealed class HeightWithGlow(name: String) : HeightMode(name) {
    open fun getGlowHeight(entity: Entity, partialTicks: Float): Double = 0.0

}


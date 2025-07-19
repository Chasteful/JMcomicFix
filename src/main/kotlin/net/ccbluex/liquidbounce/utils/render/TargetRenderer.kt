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
import net.ccbluex.liquidbounce.utils.math.Easing
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
import kotlin.math.cos
import kotlin.math.sin


/**
 * A target tracker to choose the best enemy to attack
 */

sealed class TargetRenderer<T : RenderEnvironment>(
    module: ClientModule
) : ToggleableConfigurable(module, "TargetRendering", true) {
    val colorMode = choices(this, "ColorMode", 2) {
        arrayOf(
            GenericStaticColorMode(it, Color4b(0, 128, 255, 255)),
            GenericRainbowColorMode(it),
            GenericSyncColorMode(it),
        )
    }


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
        arrayOf(Circle(module), Capture(it), Ghost())
    }

    inner class Ghost : WorldTargetRenderAppearance("Ghost") {

        private val glow = "particles/glow.png".registerAsDynamicImageFromClientResources()

        private var lastTime = System.currentTimeMillis()

        override val parent: ChoiceConfigurable<*>
            get() = appearance


        private var size by float("Size", 0.5f, 0.4f..0.7f)
        private var length by int("Length", 25, 15..40)
        private val ghostAlpha by int("Alpha", 255, 0..255)

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

                val alpha = MathHelper.clamp(ghostAlpha - (i * alphaFactor), 0, ghostAlpha)
                val renderColor = colorMode.activeChoice.getColor(mc.player).withAlpha(alpha)
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
    @Suppress("LongMethod")
    inner class Capture(override val parent: ChoiceConfigurable<*>) : WorldTargetRenderAppearance("Capture") {
        private val captureTexture by lazy {
            "particles/capture.png".registerAsDynamicImageFromClientResources()
        }


        private var currentEntity: Entity? = null
        private var previousEntity: Entity? = null
        private var lastChangeTime = 0L
        private var isFadingOut = false

        private var floatOffset = 0f
        private var prevCircleStep = 0.0
        private var circleStep = 0.0
        private var lastFrameTime = System.nanoTime()


        private val heightOffset by float("HeightOffset", 1f, 0.25f..1.75f)
        private val captureAlpha by int("Alpha", 233, 0..255)
        private val size by float("Size", 1.0f, 0.5f..1.5f)
        private val rotationSpeed by float("RotationSpeed", 20.0f, 0f..180.0f)
        private val floatRange by float("FloatRange", 0.1f, 0.05f..0.3f)
        private val floatSpeed by float("FloatSpeed", 2f, 0.5f..5f)
        private val pulseSpeed by float("PulseSpeed", 1f, 0.1f..5f)
        private val pulseRange by float("PulseRange", 0.2f, 0.1f..0.5f)

        private val slideTime by int("SlideTime", 150, 1..1000, "ms")
        private val fadeOutTime by int("FadeOutTime", 500, 1..1000, "ms")
        private val slideEasing by curve("SlideEasing", Easing.LINEAR)
        private val fadeOutEasing by curve("FadeOutEasing", Easing.QUAD_OUT)

        private val deltaTime: Double
            get() {
                val currentTime = System.nanoTime()
                val delta = (currentTime - lastFrameTime) / 1_000_000_000.0
                lastFrameTime = currentTime
                return delta.coerceAtMost(0.1)
            }

        override fun render(env: WorldRenderEnvironment, entity: Entity, partialTicks: Float) {

            if (currentEntity != entity) {
                previousEntity = currentEntity
                currentEntity = entity
                lastChangeTime = System.currentTimeMillis()
                isFadingOut = false
            }


            prevCircleStep = circleStep
            circleStep += rotationSpeed.toDouble() * deltaTime

            if (currentEntity != null) {
                renderEntity(env, currentEntity!!, partialTicks, false)
            }

            if (previousEntity != null && System.currentTimeMillis() - lastChangeTime < slideTime + fadeOutTime) {
                renderEntity(env, previousEntity!!, partialTicks, true)
            }
        }

        private fun renderEntity(
            env: WorldRenderEnvironment, entity: Entity, partialTicks: Float, isFadingOut: Boolean) {
            val currentTime = System.currentTimeMillis()
            val timeSinceChange = currentTime - lastChangeTime

            val alpha = if (isFadingOut) {
                val fadeFactor = fadeOutEasing.getFactor(
                    lastChangeTime + slideTime,
                    currentTime,
                    fadeOutTime.toFloat()
                )
                (captureAlpha * (1f - fadeFactor)).toInt().coerceAtLeast(0)
            } else {
                captureAlpha
            }

            if (isFadingOut && alpha <= 0) return

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

            val interpolatedStep = MathHelper.lerp(partialTicks.toDouble(), prevCircleStep, circleStep)

            val targetPos = entity.pos.interpolate(entity.lastRenderPos(), partialTicks.toDouble())
                .add(0.0, heightOffset.toDouble(), 0.0)

            val renderPos = if (isFadingOut || previousEntity == null || timeSinceChange >= slideTime) {
                targetPos
            } else {
                val previousPos = previousEntity!!.pos.interpolate(
                    previousEntity!!.lastRenderPos(), partialTicks.toDouble())
                    .add(0.0, heightOffset.toDouble(), 0.0)

                val factor = slideEasing.getFactor(lastChangeTime, currentTime, slideTime.toFloat()).toDouble()
                Vec3d(
                    MathHelper.lerp(factor, previousPos.x, targetPos.x),
                    MathHelper.lerp(factor, previousPos.y, targetPos.y),
                    MathHelper.lerp(factor, previousPos.z, targetPos.z)
                )
            }


            val pulseFactor = (sin(
                System.currentTimeMillis() * 0.001 * pulseSpeed) * pulseRange + 1.0).toFloat()
            val currentSize = size * pulseFactor *
                (if (isFadingOut) (1f - (timeSinceChange - slideTime).toFloat() / fadeOutTime) else 1f)

            with(renderPos) {
                floatOffset = (sin(System.currentTimeMillis() * 0.001 * floatSpeed) * floatRange).toFloat()
                env.matrixStack.translate(this.x, this.y + floatOffset, this.z)
            }

            env.matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.camera.yaw))
            env.matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.camera.pitch))
            env.matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(interpolatedStep.toFloat()))

            RenderSystem.setShaderTexture(0, captureTexture)


            with(env) {
                drawParticle(currentSize, alpha)
            }

            RenderSystem.depthMask(true)
            RenderSystem.defaultBlendFunc()
            mc.gameRenderer.lightmapTextureManager.enable()
            RenderSystem.enableCull()
            env.matrixStack.pop()
        }

        private fun WorldRenderEnvironment.drawParticle(currentSize: Float, alpha: Int) {
            val mainColor = colorMode.activeChoice.getColor(mc.player).withAlpha(alpha)
            val secondaryColor = colorMode.activeChoice.getColor(mc.player, 90).withAlpha((alpha * 0.8).toInt())
            val tertiaryColor = colorMode.activeChoice.getColor(mc.player, 180).withAlpha((alpha * 0.6).toInt())
            val quaternaryColor = colorMode.activeChoice.getColor(mc.player, 270).withAlpha((alpha * 0.8).toInt())

            drawCustomMesh(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR,
                ShaderProgramKeys.POSITION_TEX_COLOR
            ) { matrix ->
                // Top-left vertex
                vertex(matrix, -currentSize / 2, -currentSize / 2, 0.0f)
                    .texture(0.0f, 1.0f)
                    .color(mainColor.toARGB())

                // Bottom-left vertex
                vertex(matrix, -currentSize / 2, currentSize / 2, 0.0f)
                    .texture(0.0f, 0.0f)
                    .color(secondaryColor.toARGB())

                // Bottom-right vertex
                vertex(matrix, currentSize / 2, currentSize / 2, 0.0f)
                    .texture(1.0f, 0.0f)
                    .color(tertiaryColor.toARGB())

                // Top-right vertex
                vertex(matrix, currentSize / 2, -currentSize / 2, 0.0f)
                    .texture(1.0f, 1.0f)
                    .color(quaternaryColor.toARGB())
            }
        }
    }

    inner class Circle(module: ClientModule) : WorldTargetRenderAppearance("GlowingCircle") {
        override val parent: ChoiceConfigurable<*>
            get() = appearance

        private val radius by float("Radius", 0.85f, 0.1f..2f)

        private val heightMode = choices(module, "HeightMode") {
            arrayOf(FeetHeight(it), TopHeight(it), RelativeHeight(it), HealthHeight(it), AnimatedHeight(it))
        }
        private val outerAlpha by int("OuterAlpha", 80, 0..255)
        private val glowAlpha by int("GlowAlpha", 0, 0..255)

        private val glowHeightSetting by float("GlowHeight", 0.3f, -1f..1f)

        override fun render(env: WorldRenderEnvironment, entity: Entity, partialTicks: Float) {
            val height = heightMode.activeChoice.getHeight(entity, partialTicks)
            val pos = entity.interpolateCurrentPosition(partialTicks) + Vec3d(0.0, height, 0.0)

            val currentHeightMode = heightMode.activeChoice

            val glowHeight = if (currentHeightMode is HeightWithGlow) {
                currentHeightMode.getGlowHeight(entity, partialTicks) - height
            } else {
                glowHeightSetting.toDouble()
            }

            val (color, glowColor) = colorMode.activeChoice.getColors(mc.player).let {
                it.first.withAlpha(outerAlpha) to it.second.withAlpha(glowAlpha)
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
                }
            }
        }
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

sealed class TargetRenderAppearance<T : RenderEnvironment>(name: String) : Choice(name) {
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


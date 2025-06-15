package net.ccbluex.jmcomicfix.features.module.modules.render

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.ObjectLongMutablePair
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.UV2f
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.minecraft.client.render.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.pow

object ModuleJumpCircle : ClientModule("JumpCircle", Category.RENDER) {
    private val mode by enumChoice("Mode", Image.CIRCLE)
    private val onlySelf by boolean("OnlySelf", false)
    private val circleScale by float("CircleScale", 1f, 0.5f..5f)
    private val easeOut by boolean("EaseOut", true)
    private val rotateSpeed by float("RotateSpeed", 2f, 0.5f..5f)

    private val circles = ArrayDeque<ObjectLongMutablePair<Vec3d>>()
    private val cache = mutableListOf<PlayerEntity>()

    init {
        Image.entries.forEach { it.texture }
    }

    val tickHandler = tickHandler {
        val world = mc.world ?: return@tickHandler
        val players = if (onlySelf) listOfNotNull(mc.player) else world.players

        players.filter { it.isOnGround && !cache.contains(it) }.forEach { cache.add(it) }


        cache.removeAll { player ->
            if (!player.isOnGround && players.contains(player)) {
                circles.add(ObjectLongMutablePair.of(player.pos, System.currentTimeMillis()))
                true
            } else false
        }
        circles.removeAll {
            System.currentTimeMillis() - it.rightLong() > if (easeOut) 5000 else 6000
        }
    }
    val renderHandler = handler<WorldRenderEvent> { event ->
        if (circles.isEmpty()) return@handler

        val currentTime = System.currentTimeMillis()

        RenderSystem.disableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(
            GlStateManager.SrcFactor.SRC_ALPHA,
            GlStateManager.DstFactor.ONE
        )
        RenderSystem.setShader(VertexInputType.PosTexColor.shaderProgram)
        RenderSystem.setShaderTexture(0, mode.texture)

        renderEnvironmentForWorld(event.matrixStack) {
            val builder = RenderBufferBuilder(
                drawMode = VertexFormat.DrawMode.QUADS,
                vertexFormat = VertexInputType.PosTexColor,
                tesselator = RenderBufferBuilder.TESSELATOR_A
            )

            circles.forEach { circle ->
                val pos = circle.left()
                val elapsed = (currentTime - circle.rightLong()).toFloat()
                val progress = elapsed / 6000f
                val sizeAnim = circleScale * if (easeOut) {
                    1 - (1 - (elapsed * 2f) / 5000f).pow(4)
                } else {
                    elapsed / 5000f
                }
                val alpha = 1f - progress
                val rotation = sizeAnim * rotateSpeed * 1000f

                withPositionRelativeToCamera(pos) {
                    matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f))
                    matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation))

                    val scale = sizeAnim * 2f
                    val half = scale / 2f
                    val minX = -half
                    val minY = -half
                    val maxX = half
                    val maxY = half

                    builder.drawQuad(
                        this,
                        pos1 = Vec3d(minX.toDouble(), minY.toDouble(), 0.0),
                        uv1 = UV2f(0f, 1f),
                        pos2 = Vec3d(maxX.toDouble(), maxY.toDouble(), 0.0),
                        uv2 = UV2f(1f, 0f),
                        color = Color4b(255, 255, 255, (alpha * 255).toInt())
                    )
                }
            }

            builder.draw()
        }

        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
    }


    @Suppress("UNUSED")
    private enum class Image(
        override val choiceName: String,
        texturePath: String
    ) : NamedChoice {
        CIRCLE("Default", "image/jumpCircle/circle.png"),
        UWUMOUSE("UWU mouse", "image/mouse.png");

        val texture: Identifier by lazy { texturePath.registerAsDynamicImageFromClientResources() }
    }
}

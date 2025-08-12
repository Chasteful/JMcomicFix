package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.ObjectFloatMutablePair
import it.unimi.dsi.fastutil.objects.ObjectFloatPair
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.GenericCustomColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*

object ModuleBreadcrumbs : ClientModule("Breadcrumbs", Category.RENDER, aliases = arrayOf("PlayerTrails")) {

    private val onlyOwn by boolean("OnlyOwn", true)
    private val height by float("Height", 0.5f, 0f..2f)

    private val colorModes = choices(this, "ColorMode", 3) {
        arrayOf(
            GenericCustomColorMode(it, Color4b.WHITE.with(a = 80), Color4b.WHITE.with(a = 100)),
            GenericStaticColorMode(it, Color4b.WHITE.with(a = 100)),
            GenericRainbowColorMode(it),
            GenericSyncColorMode(it),
        )
    }


    private object TemporaryConfigurable : ToggleableConfigurable(this, "Temporary", true) {
        val alive by int("Alive", 900, 10..10000, "ms")
        val fade by boolean("Fade", true)
    }

    init {
        tree(TemporaryConfigurable)
    }

    private val trails = IdentityHashMap<Entity, Trail>()
    private val lastPositions = IdentityHashMap<Entity, DoubleArray>()

    override fun onDisabled() {
        clear()
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        renderEnvironmentForWorld(matrixStack) {

            val (colorStart, colorEnd) = colorModes.activeChoice.getColors(player)
            draw(matrixStack, colorStart, colorEnd)
        }
    }

    private fun draw(matrixStack: MatrixStack, startColor: Color4b, endColor: Color4b) {
        if (trails.isEmpty()) {
            return
        }
        if (height > 0) {
            RenderSystem.disableCull()
        }
        val matrix = matrixStack.peek().positionMatrix

        @Suppress("SpellCheckingInspection")
        val tessellator = RenderSystem.renderThreadTesselator()
        val camera = mc.entityRenderDispatcher.camera ?: return
        val time = System.currentTimeMillis()
        val colorStartF = Vector4f(
            startColor.r / 255f,
            startColor.g / 255f,
            startColor.b / 255f,
            startColor.a / 255f
        )
        val colorEndF = Vector4f(
            endColor.r / 255f,
            endColor.g / 255f,
            endColor.b / 255f,
            endColor.a / 255f
        )
        val lines = height == 0f
        val buffer = tessellator.begin(
            if (lines) DrawMode.DEBUG_LINES else DrawMode.QUADS,
            VertexFormats.POSITION_COLOR
        )
        val renderData = RenderData(matrix, buffer, colorStartF, colorEndF, lines)

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

        trails.forEach { (entity, trail) ->
            trail.verifyAndRenderTrail(renderData, camera, entity, time)
        }

        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)

        if (height > 0) RenderSystem.enableCull()
    }

    @Suppress("unused")
    val updateHandler = handler<GameTickEvent> {
        val time = System.currentTimeMillis()
        if (onlyOwn) {
            updateEntityTrail(time, player)
            trails.keys.retainAll { it === player || !it.isAlive }
            return@handler
        }
        val actualPresent = world.players
        actualPresent.forEach { p -> updateEntityTrail(time, p) }
        trails.keys.removeIf { key -> actualPresent.none { it === key } || !key.isAlive }
    }

    private fun updateEntityTrail(time: Long, entity: Entity) {
        val last = lastPositions[entity]
        if (last != null && entity.x == last[0] && entity.y == last[1] && entity.z == last[2]) return
        lastPositions[entity] = doubleArrayOf(entity.x, entity.y, entity.z)
        trails.getOrPut(entity, ::Trail).positions.add(TrailPart(entity.x, entity.y, entity.z, time))
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        clear()
    }

    private fun clear() {
        lastPositions.clear()
        trails.clear()
    }

    @JvmRecord
    private data class TrailPart(val x: Double, val y: Double, val z: Double, val creationTime: Long)

    private class RenderData(
        val matrix: Matrix4f,
        val bufferBuilder: BufferBuilder,
        val colorStart: Vector4f,
        val colorEnd: Vector4f,
        val lines: Boolean
    )

    private class Trail {
        val positions = ArrayDeque<TrailPart>()

        fun verifyAndRenderTrail(
            renderData: RenderData,
            camera: Camera,
            entity: Entity,
            time: Long
        ) {
            val aliveDurationF = TemporaryConfigurable.alive.toFloat()
            val initialAlphaStart = renderData.colorStart.w
            val initialAlphaEnd = renderData.colorEnd.w

            if (TemporaryConfigurable.enabled) {
                val aliveDuration = TemporaryConfigurable.alive.toLong()
                val expirationTime = time - aliveDuration
                while (positions.isNotEmpty() && positions.peekFirst().creationTime < expirationTime) {
                    positions.removeFirst()
                }
            }

            if (positions.isEmpty()) {
                return
            }

            val shouldFade = TemporaryConfigurable.fade && TemporaryConfigurable.enabled
            val pointsWithAlpha = positions.mapIndexed { index, position ->
                val alphaStart = if (shouldFade) {
                    val deltaTime = time - position.creationTime
                    val multiplier = (1F - deltaTime.toFloat() / aliveDurationF)
                    multiplier * initialAlphaStart
                } else {
                    initialAlphaStart
                }
                val alphaEnd = if (shouldFade) {
                    val deltaTime = time - position.creationTime
                    val multiplier = (1F - deltaTime.toFloat() / aliveDurationF)
                    multiplier * initialAlphaEnd
                } else {
                    initialAlphaStart
                }
                val point = calculatePoint(camera, position.x, position.y, position.z)
                Triple(point, alphaStart, alphaEnd)
            }.toMutableList()

            val interpolatedPos = entity.getLerpedPos(mc.renderTickCounter.getTickDelta(true))
            val lastPoint = calculatePoint(camera, interpolatedPos.x, interpolatedPos.y, interpolatedPos.z)
            val lastIndex = pointsWithAlpha.size - 1
            if (lastIndex >= 0) {
                val (_, oldAlphaStart, oldAlphaEnd) = pointsWithAlpha[lastIndex]
                pointsWithAlpha[lastIndex] = Triple(lastPoint, oldAlphaStart, oldAlphaEnd)
            }

            addVerticesToBuffer(renderData, pointsWithAlpha)
        }

        private fun calculatePoint(camera: Camera, x: Double, y: Double, z: Double): Vector3f {
            val point = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
            point.sub(camera.pos.x.toFloat(), camera.pos.y.toFloat(), camera.pos.z.toFloat())
            return point
        }

        private fun interpolateColor(
            t: Float,
            colorStart: Vector4f,
            colorEnd: Vector4f,
            alphaStart: Float,
            alphaEnd: Float
        ): Vector4f {
            return Vector4f(
                colorStart.x + (colorEnd.x - colorStart.x) * t,
                colorStart.y + (colorEnd.y - colorStart.y) * t,
                colorStart.z + (colorEnd.z - colorStart.z) * t,
                alphaStart + (alphaEnd - alphaStart) * t
            )
        }

        private fun addVerticesToBuffer(
            renderData: RenderData,
            list: List<Triple<Vector3f, Float, Float>>
        ) {
            val buffer = renderData.bufferBuilder
            val size = list.size
            if (size < 2) return

            for (i in 1 until size) {
                val (v0, alpha0Start, alpha0End) = list[i]
                val (v1, alpha1Start, alpha1End) = list[i - 1]

                val t0 = (i.toFloat() / (size - 1))
                val t1 = ((i - 1).toFloat() / (size - 1))

                val c0 = interpolateColor(t0, renderData.colorStart, renderData.colorEnd, alpha0Start, alpha0End)
                val c1 = interpolateColor(t1, renderData.colorStart, renderData.colorEnd, alpha1Start, alpha1End)

                buffer.vertex(renderData.matrix, v0.x, v0.y, v0.z).color(c0.x, c0.y, c0.z, c0.w)
                buffer.vertex(renderData.matrix, v1.x, v1.y, v1.z).color(c1.x, c1.y, c1.z, c1.w)
                if (!renderData.lines) {
                    buffer.vertex(renderData.matrix, v1.x, v1.y + height, v1.z).color(c1.x, c1.y, c1.z, c1.w)
                    buffer.vertex(renderData.matrix, v0.x, v0.y + height, v0.z).color(c0.x, c0.y, c0.z, c0.w)
                }
            }
        }
    }
}

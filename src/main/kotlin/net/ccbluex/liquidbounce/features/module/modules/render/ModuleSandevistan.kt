@file:Suppress("UNCHECKED_CAST")
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WireframeRenderer
import net.ccbluex.liquidbounce.utils.render.WireframeSnapshot
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.util.ArrayDeque
import java.util.IdentityHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

object ModuleSandevistan : ClientModule("Sandevistan", ModuleCategories.RENDER) {
    private val fillAlpha by int("FillAlpha", 10, 0..255)
    private val outlineAlpha by int("OutlineAlpha", 25, 0..255)

    private val delayTicks by int("DelayTicks", 2, 1..20)
    private val aliveTicks by int("AliveTicks", 20, 10..200)
    private val colorModes = choices("ColorMode", 0) {
        arrayOf(
            GenericStaticColorMode(it, Color4b.WHITE.with(a = 100)),
            GenericRainbowColorMode(it)
        )
    }
    private val lastPositions = IdentityHashMap<LivingEntity, DoubleArray>()

    private val entityTrails = IdentityHashMap<LivingEntity, ArrayDeque<WireframeSnapshot>>()
    private var pooledSnapshots = arrayOfNulls<WireframeSnapshot>(0)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val world = mc.level ?: return@handler
        val currentTime = world.gameTime
        val partialTicks = event.partialTicks

        RenderedEntities.forEach { entity ->
            val last = lastPositions[entity]
            val cx = entity.x; val cy = entity.y; val cz = entity.z

            if (last == null || last[0] != cx || last[1] != cy || last[2] != cz) {
                lastPositions[entity] = doubleArrayOf(cx, cy, cz)

                val trail = entityTrails.getOrPut(entity) { ArrayDeque() }

                val bodyYaw = run {
                    if (RotationManager.currentRotation != null && entity == mc.player && ModuleRotations.running) {
                        Mth.rotLerp(partialTicks, RotationManager.previousRotation?.yaw ?: entity.yBodyRotO, RotationManager.currentRotation!!.yaw)
                    } else {
                        Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot)
                    }
                }

                val currentPos = entity.interpolateCurrentPosition(partialTicks)

                val renderer = mc.entityRenderDispatcher.getRenderer(entity)

                val unsafeRenderer = renderer as EntityRenderer<Entity, EntityRenderState>

                val currentRenderState = unsafeRenderer.createRenderState(entity, partialTicks).apply {
                    unsafeRenderer.extractRenderState(entity, this, partialTicks)
                }

                trail.addFirst(
                    WireframeSnapshot(
                        currentPos,
                        currentTime,
                        bodyYaw,
                        entity.yRot,
                        entity.xRot,
                        entity,
                        currentRenderState
                    )
                )
            }
        }

        entityTrails.forEach { (_, trail) ->
            while (trail.isNotEmpty() && currentTime - trail.peekLast().creationTime > aliveTicks) {
                trail.removeLast()
            }
        }
        entityTrails.keys.retainAll { it.isAlive }
        lastPositions.keys.retainAll { it.isAlive }

        if (entityTrails.isEmpty()) return@handler

        var totalSnapshots = 0
        entityTrails.forEach { (_, trail) ->
            if (trail.size > delayTicks) {
                totalSnapshots += (trail.size - delayTicks)
            }
        }
        if (totalSnapshots == 0) return@handler

        if (pooledSnapshots.size < totalSnapshots) {
            pooledSnapshots = arrayOfNulls(totalSnapshots)
        }

        var writeIndex = 0
        entityTrails.forEach { (_, trail) ->

            for ((idx, snapshot) in trail.withIndex()) {
                if (idx >= delayTicks) {
                    pooledSnapshots[writeIndex++] = snapshot
                }
                if (writeIndex >= totalSnapshots) break
            }
        }

        renderEnvironmentForWorld(event.matrixStack) {
            for (i in 0 until totalSnapshots) {
                val snapshot = pooledSnapshots[i] ?: continue
                val entity = snapshot.entity

                val lifeAge = currentTime - snapshot.creationTime
                val progress = 1f - (lifeAge.toFloat() / aliveTicks.toFloat()).coerceIn(0f, 1f)
                if (progress <= 0f) continue

                val baseColor = colorModes.activeMode.getColor(entity)
                val fillColor = baseColor.alpha((fillAlpha * progress).toInt())
                val outlineColor = baseColor.alpha((outlineAlpha * progress).toInt())

                WireframeRenderer.renderSnapshot(event, this, snapshot, fillColor, outlineColor)
            }
        }
    }

    override fun onEnabled() {
        entityTrails.clear()
        lastPositions.clear()
    }

    override fun onDisabled() {
        entityTrails.clear()
        lastPositions.clear()
    }
}

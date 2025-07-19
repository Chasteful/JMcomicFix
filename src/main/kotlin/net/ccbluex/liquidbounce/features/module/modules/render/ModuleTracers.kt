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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.MathHelper
import java.awt.Color

/**
 * Tracers module
 *
 * Draws a line to every entity a certain radius.
 */

object ModuleTracers : ClientModule("Tracers", Category.RENDER) {

    private val modes = choices("ColorMode", 4) {
        arrayOf(
            DistanceColor,
            GenericCustomColorMode(it, Color4b.WHITE.with(a = 80), Color4b.WHITE.with(a = 100)),
            GenericStaticColorMode(it, Color4b(0, 160, 255, 255)),
            GenericRainbowColorMode(it),
            GenericSyncColorMode(it),
        )
    }

    private object DistanceColor : GenericColorMode<LivingEntity>("Distance") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        val useViewDistance by boolean("UseViewDistance", true)
        val customViewDistance by float("CustomViewDistance", 128.0F, 1.0F..512.0F)

        override fun getColors(param: LivingEntity): Pair<Color4b, Color4b> {
            return Color4b(255, 0, 0, 255) to Color4b(0, 255, 0, 255)
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val useDistanceColor = DistanceColor.isSelected

        val viewDistance = 16.0F * MathHelper.SQUARE_ROOT_OF_TWO *
            (if (DistanceColor.useViewDistance) {
                mc.options.viewDistance.value.toFloat()
            } else {
                DistanceColor.customViewDistance
            })
        val entities = world.entities.filter(this::shouldRenderTrace)
        val camera = mc.gameRenderer.camera

        if (entities.isEmpty()) return@handler

        renderEnvironmentForWorld(matrixStack) {
            val eyeVec = Vec3(0.0, 0.0, 1.0)
                .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
                .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat())

            longLines {
                for (entity in entities) {
                    if (entity !is LivingEntity) continue

                    val dist = player.distanceTo(entity) * 2.0F
                    val tagColor = EntityTaggingManager.getTag(entity).color
                    val friendColor = if (entity is PlayerEntity &&
                        FriendManager.isFriend(entity.gameProfile.name)
                    ) {
                        Color4b.BLUE
                    } else {
                        null
                    }
                    val activeMode = modes.activeChoice
                    val (startColor, endColor) = when (activeMode) {
                        is GenericCustomColorMode -> activeMode.getColors(entity)
                        is GenericSyncColorMode -> activeMode.getColors(entity)
                        else -> {
                            val baseColor = tagColor ?: friendColor ?: if (useDistanceColor) {
                                Color4b(
                                    Color.getHSBColor(
                                        (dist.coerceAtMost(viewDistance) / viewDistance) * (120.0f / 360.0f),
                                        1.0f, 1.0f
                                    )
                                )
                            } else {
                                activeMode.getColor(entity)
                            }
                            baseColor to baseColor
                        }
                    }

                    val entityPos = relativeToCamera(entity.interpolateCurrentPosition(event.partialTicks)).toVec3()
                    val entityTop = entityPos + Vec3(0f, entity.height, 0f)

                    if (startColor == endColor) {
                        withColor(startColor) {
                            drawLines(eyeVec, entityPos, entityPos, entityTop)
                        }
                    } else {

                        val segments = 10
                        val points = List(segments + 1) { i ->
                            val t = i / segments.toDouble()
                            Vec3(
                                eyeVec.x + (entityPos.x - eyeVec.x) * t,
                                eyeVec.y + (entityPos.y - eyeVec.y) * t,
                                eyeVec.z + (entityPos.z - eyeVec.z) * t
                            )
                        }

                        for (i in 0 until segments) {
                            val t = i / (segments - 1.0)
                            val color = startColor.interpolateTo(endColor, t)
                            withColor(color) {
                                drawLines(points[i], points[i + 1])
                            }
                        }

                        withColor(endColor) {
                            drawLines(entityPos, entityTop)
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun shouldRenderTrace(entity: Entity) = entity.shouldBeShown()
}

@file:Suppress("UNCHECKED_CAST")
package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.EnderDragonEntityRendererAccessor
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.LivingEntityRendererAccessor
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.`object`.equipment.ElytraModel
import net.minecraft.client.model.player.PlayerCapeModel
import net.minecraft.client.renderer.entity.EnderDragonRenderer
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.entity.state.SlimeRenderState
import net.minecraft.client.renderer.entity.state.WitherRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

data class WireframeEntity(
    private var pos: Vec3,
    private var yaw: Float,
    private var pitch: Float,
    private val entity: Entity
) {
    private val renderer by lazy {
        mc.entityRenderDispatcher.getRenderer(entity) as EntityRenderer<Entity, EntityRenderState>
    }

    private val model: EntityModel<EntityRenderState>? by lazy {
        when (val r = renderer) {
            is EnderDragonRenderer -> {
                (r as EnderDragonEntityRendererAccessor).model as? EntityModel<EntityRenderState>
            }
            else -> (r as? RenderLayerParent<*, *>)?.model as? EntityModel<EntityRenderState>
        }
    }

    @Suppress("CognitiveComplexMethod", "LongMethod")
    fun render(event: WorldRenderEvent, color: Color4b, outlineColor: Color4b) {
        renderEnvironmentForWorld(event.matrixStack) {

            fun drawWireframe(part: ModelPart?, localPoseStack: PoseStack, color: Color4b, outlineColor: Color4b) {
                part?.visit(localPoseStack) { _, _, _, cuboid ->
                    drawBox(
                        AABB(
                            cuboid.minX / 16.0, cuboid.minY / 16.0, cuboid.minZ / 16.0,
                            cuboid.maxX / 16.0, cuboid.maxY / 16.0, cuboid.maxZ / 16.0
                        ),
                        color, outlineColor
                    )
                }
            }

            withPositionRelativeToCamera(pos) {
                poseStack.withPush {
                    val tickDelta = event.partialTicks
                    val renderState = renderer.createRenderState(entity, tickDelta)
                    renderer.extractRenderState(entity, renderState, tickDelta)

                    when (entity) {
                        is EnderDragon -> {
                            val enderState = renderState as EnderDragonRenderState
                            val f = enderState.getHistoricalPos(7).yRot
                            val g = (enderState.getHistoricalPos(5).y - enderState.getHistoricalPos(10).y).toFloat()
                            poseStack.mulPose(Axis.YP.rotationDegrees(-f))
                            poseStack.mulPose(Axis.XP.rotationDegrees(g * 10.0F))
                            poseStack.translate(0.0F, 0.0F, 1.0F)
                        }
                        else -> {
                            val living = entity as? LivingEntity ?: return@withPositionRelativeToCamera
                            val bodyYaw = if (RotationManager.currentRotation != null
                                && living == mc.player
                                && ModuleRotations.running) {
                                Mth.rotLerp(
                                    tickDelta,
                                    RotationManager.previousRotation?.yaw ?: living.yBodyRotO,
                                    RotationManager.currentRotation!!.yaw
                                )
                            } else {
                                Mth.rotLerp(tickDelta, living.yBodyRotO, living.yBodyRot)
                            }

                            val livingState = renderState as? LivingEntityRenderState
                            livingState?.let {
                                poseStack.scale(it.scale, it.scale, it.scale)
                            }
                            val rendererInstance = mc.entityRenderDispatcher.getRenderer(entity)
                            val livingRendererInstance =
                                rendererInstance as? LivingEntityRenderer<LivingEntity, LivingEntityRenderState, *>

                            livingRendererInstance?.let {
                                livingState?.let { it1 ->
                                    (it as LivingEntityRendererAccessor<LivingEntity, LivingEntityRenderState>)
                                        .invokeSetupRotations(
                                            livingState,
                                            poseStack,
                                            bodyYaw,
                                            it1.scale
                                        )
                                }
                            }
                        }
                    }

                    when (renderState) {
                        is SlimeRenderState -> {
                            val size = renderState.size
                            val squish = renderState.squish
                            val inv = 1.0f / (squish + 1.0f)
                            poseStack.scale(size * inv, size / inv, size * inv)
                        }
                        is WitherRenderState -> {
                            var f = 2.0F
                            if (renderState.invulnerableTicks > 0.0F) {
                                f -= renderState.invulnerableTicks / 220.0F * 0.5F
                            }
                            poseStack.scale(f, f, f)
                        }
                        else -> {
                            val defaultScale = when (entity) {
                                is Player -> 0.9375f
                                else -> 1.0f
                            }
                            poseStack.scale(defaultScale, defaultScale, defaultScale)
                        }
                    }

                    poseStack.scale(-1f, -1f, 1f)
                    poseStack.translate(0.0, -1.501, 0.0)

                    model?.setupAnim(renderState)
                    drawWireframe(model?.root(), poseStack, color, outlineColor)

                    if (entity is Player && renderState is HumanoidRenderState) {
                        val entityModelSet = mc.entityModels
                        val entry = network.getPlayerInfo(entity.uuid)
                        val skin = entry?.skin

                        if (skin?.cape != null) {
                            val capePart = entityModelSet.bakeLayer(ModelLayers.PLAYER_CAPE)
                            val capeModel = PlayerCapeModel(capePart)
                            val avatarState = renderState as? AvatarRenderState
                            avatarState?.let {
                                capeModel.setupAnim(it)
                            }
                            poseStack.withPush {
                                drawWireframe(capePart, poseStack, color, outlineColor)
                            }
                        }

                        val elytraStack = entity.getItemBySlot(EquipmentSlot.CHEST)
                        val hasElytra = elytraStack.`is`(Items.ELYTRA) && elytraStack.has(DataComponents.EQUIPPABLE)
                        if (hasElytra) {
                            val elytraPart = entityModelSet.bakeLayer(ModelLayers.ELYTRA)
                            val elytraModel = ElytraModel(elytraPart)
                            elytraModel.setupAnim(renderState)
                            poseStack.withPush {
                                drawWireframe(elytraPart, poseStack, color, outlineColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

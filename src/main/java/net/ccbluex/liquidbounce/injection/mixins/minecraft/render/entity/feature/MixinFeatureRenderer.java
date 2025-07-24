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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.entity.feature;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleChams;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleLogoffSpot;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTrueSight;
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FeatureRenderer.class)
public abstract class MixinFeatureRenderer {

    @Unique
    private static final int ESP_TRUE_SIGHT_REQUIREMENT_COLOR = new Color4b(255, 255, 255, 255).alpha(120).toARGB();

    @WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private static void injectChamsFeatureColor(EntityModel instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, int overlay, int color, Operation<Void> original, @Local(argsOnly = true) LivingEntityRenderState state) {
        var trueSightModule = ModuleTrueSight.INSTANCE;
        var chams = ModuleChams.INSTANCE;

        if (ModuleTrueSight.canRenderEntities(state)) {
            color = trueSightModule.getRunning() && trueSightModule.getEntities()
                    ? trueSightModule.getEntityFeatureLayerColor().toARGB()
                    : ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
        } else if (ModuleLogoffSpot.INSTANCE.isLogoffEntity(state)) {
            color = ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
        } else if (chams.getRunning()) {
            var entity = ((EntityRenderStateAddition) state).liquid_bounce$getEntity();
            if (!entity.isInvisible()) {
                color = chams.getColor((LivingEntity) entity).toARGB();
            }
        }

        original.call(instance, matrixStack, vertexConsumer, light, overlay, color);
    }

    @WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getEntityCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"))
    private static RenderLayer injectChamsFeatureRenderLayer(Identifier texture, Operation<RenderLayer> original, @Local(argsOnly = true) LivingEntityRenderState state) {
        if (ModuleTrueSight.canRenderEntities(state)
                || ModuleLogoffSpot.INSTANCE.isLogoffEntity(state)
                || (ModuleChams.INSTANCE.getRunning() && !((EntityRenderStateAddition) state).liquid_bounce$getEntity().isInvisible())) {
            return RenderLayer.getItemEntityTranslucentCull(texture);
        }
        return original.call(texture);
    }


}

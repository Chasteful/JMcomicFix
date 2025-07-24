package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleChams;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTrueSight;
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
@Mixin(EquipmentRenderer.class)
public class EquipmentRendererMixin {

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getArmorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private RenderLayer modifyRenderLayer(
            RenderLayer original,
            @Local(argsOnly = true) EquipmentModel.LayerType layerType,
            @Local(argsOnly = true) RegistryKey<EquipmentAsset> assetKey,
            @Local(argsOnly = true) Model model,
            @Local(argsOnly = true) VertexConsumerProvider vertexConsumers,
            @Local(name = "identifier", ordinal = 0, argsOnly = true) Identifier identifier
    ) {
        LivingEntity entity = getCurrentEntity(vertexConsumers);
        if (entity != null && vertexConsumers instanceof EntityRenderStateAddition add) {
            if (ModuleChams.INSTANCE.getRunning() && !entity.isInvisible()) {
                return RenderLayer.getItemEntityTranslucentCull(identifier);
            } else if (ModuleTrueSight.canRenderEntities((LivingEntityRenderState) add)) {
                return RenderLayer.getItemEntityTranslucentCull(identifier);
            }
        }
        return original;
    }


    @ModifyArg(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
                    ordinal = 0
            ),
            index = 4
    )
    private int modifyAlpha(int color, @Local(argsOnly = true) VertexConsumerProvider vcp) {
        LivingEntity entity = getCurrentEntity(vcp);
        if (entity != null) {
            if (vcp instanceof EntityRenderStateAddition add) {
                if (ModuleChams.INSTANCE.getRunning() && !entity.isInvisible()) {
                    Color4b c = ModuleChams.INSTANCE.getColor(entity);
                    return ColorHelper.withAlpha(ModuleChams.INSTANCE.getAlpha(), c.toARGB());
                } else if (ModuleTrueSight.canRenderEntities((LivingEntityRenderState) add)) {
                    Color4b c = ModuleTrueSight.INSTANCE.getEntityColor();
                    return ColorHelper.withAlpha(c.a(), c.toARGB());
                }
            }
        }
        return color;
    }

    @Unique
    private LivingEntity getCurrentEntity(VertexConsumerProvider vcp) {
        if (vcp instanceof EntityRenderStateAddition add) {
            if (add.liquid_bounce$getEntity() instanceof LivingEntity le) {
                return le;
            }
        }
        return null;
    }
}

package net.ccbluex.liquidbounce.injection.mixins.minecraft.block;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.ccbluex.liquidbounce.features.module.modules.exploit.movefix.ModuleMovePhysics;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.ShapeContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public abstract class MixinAbstractBlock {
    @Unique
    private static final Supplier<ModuleMovePhysics> MINIBLOX = Suppliers.memoize(() -> ModuleMovePhysics.INSTANCE);

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void onGetCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        ModuleMovePhysics dis = MINIBLOX.get();
        if (state.getBlock() instanceof ChestBlock && dis.getRunning()) {
            cir.setReturnValue(VoxelShapes.fullCube());
        }
    }

    @Inject(method = "getOutlineShape", at = @At("HEAD"), cancellable = true)
    private void onGetOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (state.getBlock() instanceof ChestBlock && ModuleMovePhysics.INSTANCE.getRunning()) {
            cir.setReturnValue(VoxelShapes.fullCube());
        }
    }
}

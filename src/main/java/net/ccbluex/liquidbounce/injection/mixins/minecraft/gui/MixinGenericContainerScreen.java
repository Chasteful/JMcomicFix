package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class MixinGenericContainerScreen {

    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GenericContainerScreen screen = (GenericContainerScreen) (Object) this;
        Text title = screen.getTitle();

        if (!ModuleChestStealer.INSTANCE.getRunning() || !ModuleChestStealer.INSTANCE.getCheckTitle()) {
            return;
        }

        DoRender renderType = null;
        String titleString = title.getString();

        if (titleString.equals(Text.translatable("container.chest").getString())) {
            renderType = DoRender.CHEST;
        } else if (titleString.equals(Text.translatable("container.chestDouble").getString())) {
            renderType = DoRender.CHEST_LAGER;
        } else if (titleString.equals(Text.translatable("container.enderchest").getString())) {
            renderType = DoRender.ENDER_CHEST;
        } else if (titleString.equals(Text.translatable("container.shulkerBox").getString())) {
            renderType = DoRender.SHULKER_BOX;
        } else if (titleString.equals(Text.translatable("container.barrel").getString())) {
            renderType = DoRender.BARREL;
        }


        if (renderType != null && !ModuleChestStealer.canRender(renderType)) {
            ci.cancel();
        }
    }
}

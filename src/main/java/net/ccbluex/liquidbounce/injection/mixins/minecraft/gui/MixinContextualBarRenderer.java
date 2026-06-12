package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContextualBarRenderer.class)
public interface MixinContextualBarRenderer {

    @Inject(
        method = "extractExperienceLevel",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void disableExpNumber(GuiGraphicsExtractor graphics, Font font, int experienceLevel, CallbackInfo ci) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_EXP_NUMBER)) {
            ci.cancel();
        }
    }
}

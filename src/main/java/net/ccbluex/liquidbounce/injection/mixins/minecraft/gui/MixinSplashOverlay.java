package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.common.BackgroundTexture;
import net.ccbluex.liquidbounce.common.RenderLayerExtensions;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * LiquidBounce Splash Screen
 */
@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {

    @Unique
    private static final IntSupplier CLIENT_ARGB = () -> ColorHelper.getArgb(255, 0, 0, 0);

    @Inject(method = "init", at = @At("RETURN"))
    private static void initializeTexture(TextureManager textureManager, CallbackInfo ci) {
        textureManager.registerTexture(BackgroundTexture.CLIENT_LOGO, new BackgroundTexture());
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(context, delta));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V"))
    private boolean drawMojangLogo(DrawContext instance, Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        return HideAppearance.INSTANCE.isHidingNow();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourceReload;getProgress()F"))
    private void drawClientLogo(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci,
            @Local(name = "i", index = 5) int scaledWindowWidth,
            @Local(name = "j", index = 6) int scaledWindowHeight,
            @Local(name = "s", index = 20) int color
    ) {
        if (HideAppearance.INSTANCE.isHidingNow()) return;


        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int textureWidth = BackgroundTexture.WIDTH;
        int textureHeight = BackgroundTexture.HEIGHT;

        context.drawTexture(
                RenderLayerExtensions::getSmoothTextureLayer,
                BackgroundTexture.CLIENT_LOGO,
                0, 0, 0.0F, 0.0F,
                screenWidth, screenHeight,
                textureWidth, textureHeight,
                textureWidth, textureHeight,
                color
        );

    }
    @Redirect(
            method = "renderProgressBar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
            )
    )
    private void redirectProgressBarFill(DrawContext instance, int x1, int y1, int x2, int y2, int color) {

    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/SplashOverlay;BRAND_ARGB:Ljava/util/function/IntSupplier;"))
    private IntSupplier withClientColor(IntSupplier original) {
        return HideAppearance.INSTANCE.isHidingNow() ? original : CLIENT_ARGB;
    }
}

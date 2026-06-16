package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSkinChanger;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Mixin(SkinTextureDownloader.class)
public class MixinSkinTextureDownloader {

    @Shadow @Final private TextureManager textureManager;
    @Shadow @Final private Executor mainThreadExecutor;
    @Shadow private static NativeImage processLegacySkin(NativeImage image, String url) { throw new AssertionError(); }

    @Inject(
        method = "downloadAndRegisterSkin",
        at = @At("HEAD"),
        cancellable = true
    )
    private void liquid_bounce$interceptLocalSkin(
        Identifier textureId,
        java.nio.file.Path localCopy,
        String url,
        boolean processLegacySkin,
        CallbackInfoReturnable<CompletableFuture<ClientAsset.Texture>> cir
    ) {

        if (!ModuleSkinChanger.INSTANCE.getRunning() || !url.contains("lb_local_skin")) {
            return;
        }

        cir.setReturnValue(supplyLocalSkin(textureId, url, processLegacySkin));
    }

    @Unique
    private CompletableFuture<ClientAsset.Texture> supplyLocalSkin(Identifier textureId, String url, boolean shouldProcessLegacy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes = ModuleSkinChanger.Mode.File.getCurrentSkinBytes();
                if (bytes == null) {
                    throw new IOException("Local skin bytes are unavailable.");
                }

                NativeImage nativeImage;
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                    nativeImage = NativeImage.read(bais);
                }

                if (shouldProcessLegacy) {
                    nativeImage = processLegacySkin(nativeImage, url);
                }

                return nativeImage;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Util.backgroundExecutor()).thenCompose(nativeImage -> {

            ClientAsset.Texture customTexture = new ClientAsset.Texture() {
                @Override
                public @NonNull Identifier id() {
                    return textureId;
                }

                @Override
                public @NonNull Identifier texturePath() {
                    return textureId;
                }
            };

            return CompletableFuture.supplyAsync(() -> {
                DynamicTexture dynamicTexture = new DynamicTexture(textureId::toString, nativeImage);
                this.textureManager.register(textureId, dynamicTexture);
                return customTexture;
            }, this.mainThreadExecutor);
        });
    }
}

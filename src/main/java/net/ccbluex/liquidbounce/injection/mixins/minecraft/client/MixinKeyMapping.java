/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.KeyBindingCPSEvent;
import net.ccbluex.liquidbounce.event.events.KeybindChangeEvent;
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent;
import net.ccbluex.liquidbounce.interfaces.KeyBindingAdditions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class MixinKeyMapping implements KeyBindingAdditions {

    @Shadow
    public InputConstants.Key key;

    @Unique
    private final int[] liquidbounce$countByTick = new int[20];

    @Unique
    private boolean liquidbounce$countedInOnKeyPressed = false;

    @Override
    @Unique
    public void liquidbounce$incrementCurrentCount() {
        liquidbounce$currentCount++;
        liquidbounce$countedInOnKeyPressed = true;
    }

    @Unique
    private int liquidbounce$cps = 0;

    @Unique
    private int liquidbounce$tickIndex = 0;

    @Unique
    private int liquidbounce$currentCount = 0;

    @Unique
    private boolean liquidbounce$wasPressed = false;

    @Override
    public void liquidbounce$triggerTickEnd() {
        liquidbounce$cps -= liquidbounce$countByTick[liquidbounce$tickIndex];
        liquidbounce$countByTick[liquidbounce$tickIndex] = liquidbounce$currentCount;
        liquidbounce$cps += liquidbounce$currentCount;
        liquidbounce$currentCount = 0;
        liquidbounce$tickIndex = (liquidbounce$tickIndex + 1) % liquidbounce$countByTick.length;
        // 使用更新后的 key 字段
        EventManager.INSTANCE.callEvent(new KeyBindingCPSEvent(this.key, liquidbounce$cps));
    }

    @Override
    public int liquidbounce$getCps() {
        return liquidbounce$cps;
    }

    @Inject(method = "click", at = @At("HEAD"))
    private static void onKeyPressedCPSInject(InputConstants.Key key, CallbackInfo ci) {

        for (KeyMapping kb : Minecraft.getInstance().options.keyMappings) {

            if (kb.key.getValue() == key.getValue() && kb instanceof KeyBindingAdditions) {
                ((KeyBindingAdditions) kb).liquidbounce$incrementCurrentCount();
            }
        }
    }

    @Inject(method = "setKey", at = @At("RETURN"))
    private void hookSetBoundKey(InputConstants.Key key, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(KeybindChangeEvent.INSTANCE);
    }

    @ModifyReturnValue(method = "isDown", at = @At("RETURN"))
    private boolean isPressed(boolean original) {
        boolean pressed = EventManager.INSTANCE.callEvent(new KeybindIsPressedEvent((KeyMapping) (Object) this, original)).isPressed();
        if (!liquidbounce$wasPressed && pressed && !liquidbounce$countedInOnKeyPressed) {
            liquidbounce$currentCount++;
        }
        liquidbounce$wasPressed = pressed;
        liquidbounce$countedInOnKeyPressed = false;
        return pressed;
    }
}

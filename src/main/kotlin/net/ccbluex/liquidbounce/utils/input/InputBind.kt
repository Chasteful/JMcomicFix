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
package net.ccbluex.liquidbounce.utils.input

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.emptyEnumSet
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import java.util.EnumSet

/**
 * Data class representing a key binding.
 * It holds the key to be bound and the action that will be triggered by the binding.
 *
 * @param boundKey The key that is bound to an action.
 * @param action The action triggered by the bound key (e.g., TOGGLE, HOLD).
 */
data class InputBind(
    var boundKey: InputUtil.Key,
    var action: BindAction = BindAction.TOGGLE,
    val modifiers: EnumSet<Modifier> = emptyEnumSet(),
) {

    /**
     * Alternative constructor to create a binding from the key type and key code.
     *
     * @param type The type of input (keyboard, mouse, etc.).
     * @param code The key or button code.
     * @param action The action to bind to this key.
     */
    constructor(type: InputUtil.Type, code: Int, action: BindAction) : this(type.createFromCode(code), action)

    /**
     * Constructor to create a binding using a key name.
     *
     * @param name The name of the key, which will be translated to an InputUtil.Key.
     */
    constructor(name: String) : this(inputByName(name))

    /**
     * Retrieves the name of the key in uppercase format, excluding the category prefixes.
     *
     * @return A formatted string representing the bound key's name, or "None" if unbound.
     */
    val keyName: String
        get() = when {
            isUnbound -> "None"
            else -> this.boundKey.translationKey
                .split('.')
                .drop(2) // Drops the "key.keyboard" or "key.mouse" part
                .joinToString(separator = "_") // Joins the remaining parts with underscores
                .uppercase() // Converts the key name to uppercase
        }

    /**
     * Checks if the key is unbound (i.e., set to UNKNOWN_KEY).
     *
     * @return True if the key is unbound, false otherwise.
     */
    val isUnbound: Boolean
        get() = this.boundKey == InputUtil.UNKNOWN_KEY

    /**
     * Binds to the given input name.
     */
    fun bind(name: String) {
        this.boundKey = inputByName(name)
    }

    /**
     * Binds to the given input type and code.
     */
    fun bind(key: InputUtil.Key) {
        this.boundKey = key
    }

    /**
     * Unbinds the key by setting it to UNKNOWN_KEY.
     */
    fun unbind() {
        this.boundKey = InputUtil.UNKNOWN_KEY
    }

    /**
     * Determines if the specified key matches the bound key.
     *
     * @param keyCode The GLFW key code to check.
     * @param scanCode The scan code to check.
     * @return True if the key code or scan code matches the bound key, false otherwise.
     */
    fun matchesKey(keyCode: Int, scanCode: Int): Boolean {
        return if (keyCode == InputUtil.UNKNOWN_KEY.code) {
            this.boundKey.category == InputUtil.Type.SCANCODE && this.boundKey.code == scanCode
        } else {
            this.boundKey.category == InputUtil.Type.KEYSYM && this.boundKey.code == keyCode
        }
    }

    /**
     * Determines if the specified mouse button code matches the bound key.
     *
     * @param code The mouse button code to check.
     * @return True if the mouse button matches the bound key, false otherwise.
     */
    fun matchesMouse(code: Int): Boolean {
        return this.boundKey.category == InputUtil.Type.MOUSE && this.boundKey.code == code
    }

    /**
     * Determines if the given modifiers match the required modifiers.
     *
     * @param mods The bits of modifiers.
     * @see org.lwjgl.glfw.GLFW
     */
    fun matchesModifiers(mods: Int): Boolean {
        return this.modifiers.all { it.isActive(mods) }
    }

    /**
     * Handles the event. Returns the new state, assumes the original state is `false`.
     *
     * @param event The [KeyboardKeyEvent] to handle.
     * @param currentState The current state.
     * @return The new state.
     */
    fun getNewState(event: KeyboardKeyEvent, currentState: Boolean): Boolean {
        if (!matchesKey(event.keyCode, event.scanCode)) {
            return currentState
        }

        val eventAction = event.action
        return when {
            eventAction == GLFW.GLFW_PRESS && mc.currentScreen == null -> {
                !currentState || action == BindAction.HOLD
            }

            eventAction == GLFW.GLFW_RELEASE -> false
            else -> currentState
        }
    }

    /**
     * Enum representing the action associated with a key binding.
     * It includes two actions: TOGGLE and HOLD.
     *
     * @param choiceName The display name of the action.
     */
    enum class BindAction(override val choiceName: String) : NamedChoice {
        TOGGLE("Toggle"),
        HOLD("Hold")
    }

    enum class Modifier(override val choiceName: String, val bitMask: Int, vararg val keyCodes: Int): NamedChoice {
        SHIFT("Shift", GLFW.GLFW_MOD_SHIFT, InputUtil.GLFW_KEY_LEFT_SHIFT, InputUtil.GLFW_KEY_RIGHT_SHIFT),
        CONTROL("Control", GLFW.GLFW_MOD_CONTROL, InputUtil.GLFW_KEY_LEFT_CONTROL, InputUtil.GLFW_KEY_RIGHT_CONTROL),
        ALT("Alt", GLFW.GLFW_MOD_ALT, InputUtil.GLFW_KEY_LEFT_ALT, InputUtil.GLFW_KEY_RIGHT_ALT),
        SUPER("Super", GLFW.GLFW_MOD_SUPER, InputUtil.GLFW_KEY_LEFT_SUPER, InputUtil.GLFW_KEY_RIGHT_SUPER);

        /**
         * Check if self is active in [modifiers] value.
         */
        fun isActive(modifiers: Int) = modifiers and this.bitMask != 0

        /**
         * Check if any one modifier key is pressed.
         */
        val isAnyPressed: Boolean get() = this.keyCodes.any { InputUtil.isKeyPressed(mc.window.handle, it) }

        /**
         * Performs the platform (OS) specified render name of a modifier.
         */
        val platformRenderName: String get() = when (Util.getOperatingSystem()) {
            Util.OperatingSystem.OSX -> when (this) {
                CONTROL -> "Ctrl"
                SUPER -> "\u229e"
                else -> choiceName
            }
            Util.OperatingSystem.WINDOWS -> when (this) {
                SHIFT -> "\u21e7"
                CONTROL -> "^"
                ALT -> "\u2325"
                SUPER -> "\u2318"
                // else -> choiceName
            }
            else -> choiceName
        }

        companion object {
            @JvmField
            internal val KEY_CODE_LOOKUP: Int2ObjectMap<Modifier> = run {
                val map = Int2ObjectOpenHashMap<Modifier>()
                for (modifier in Modifier.entries) {
                    for (keyCode in modifier.keyCodes) {
                        map[keyCode] = modifier
                    }
                }
                Int2ObjectMaps.unmodifiable(map)
            }

            @JvmStatic
            fun fromRawValue(modifiers: Int) = entries.filterTo(emptyEnumSet()) {
                it.isActive(modifiers)
            }
        }
    }

}

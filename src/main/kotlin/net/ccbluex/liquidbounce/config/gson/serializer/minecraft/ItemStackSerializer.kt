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

package net.ccbluex.liquidbounce.config.gson.serializer.minecraft

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.ccbluex.liquidbounce.utils.inventory.getArmorColor
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import java.lang.reflect.Type

object ItemStackSerializer : JsonSerializer<ItemStack> {
    override fun serialize(src: ItemStack?, typeOfSrc: Type, context: JsonSerializationContext) = src?.let {
        JsonObject().apply {
            addProperty("identifier", Registries.ITEM.getId(it.item).toString())
            add("displayName", context.serialize(it.name))
            addProperty("count", it.count)
            addProperty("damage", it.damage)
            addProperty("maxDamage", it.maxDamage)
            addProperty("empty", it.isEmpty)
            addProperty("hasEnchantment", !it.enchantments.isEmpty)
            val dyedColor = it.get(DataComponentTypes.DYED_COLOR)?.rgb()
                ?: it.getArmorColor()
            addProperty("hasDyedColor", dyedColor != null)
            dyedColor?.let { color ->
                addProperty("dyedColor", color)
            }
        }
    }

}

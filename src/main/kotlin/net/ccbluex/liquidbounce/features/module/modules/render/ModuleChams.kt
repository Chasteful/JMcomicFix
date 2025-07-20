package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.GenericEntityHealthColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity

object ModuleChams : ClientModule("Chams", Category.RENDER) {

    private val colorModes = choices("ColorMode", 1) {
        arrayOf(
            GenericEntityHealthColorMode(it),
            GenericStaticColorMode(it, Color4b.WHITE),
            GenericRainbowColorMode(it),
            GenericSyncColorMode(it)
        )
    }
    private val friendColor by color("Friends", Color4b.BLUE)
    private val attackColor by color("Attacked", Color4b.RED)

    private val alpha by int("TextureAlpha", 70, 0..255)

    fun getColor(entity: LivingEntity): Color4b {

        return getBaseColor(entity).withAlpha(alpha)
    }

    fun getBaseColor(entity: LivingEntity): Color4b {
        if (entity is PlayerEntity) {
            if (FriendManager.isFriend(entity) && friendColor.a > 0) {
                return friendColor
            }

            if (entity.hurtTime > 0) {
                return attackColor
            }

            EntityTaggingManager.getTag(entity).color?.let { return it }
        }

        return colorModes.activeChoice.getColor(entity)
    }
}

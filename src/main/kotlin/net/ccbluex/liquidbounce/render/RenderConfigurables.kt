package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.BlockPos

abstract class GenericColorMode<in T>(name: String) : Choice(name) {
    // 原方法
    abstract fun getColors(param: T): Pair<Color4b, Color4b>
    open fun getColor(param: T): Color4b = getColors(param).first

    // 新增带角度参数的方法
    open fun getColor(param: T, angle: Int): Color4b {
        // 默认实现忽略角度参数
        return getColor(param)
    }
}

class GenericStaticColorMode(
    override val parent: ChoiceConfigurable<*>,
    defaultColor: Color4b
) : GenericColorMode<Any?>("Static") {

    private val staticColor by color("Color", defaultColor)

    override fun getColors(param: Any?) = staticColor to staticColor
}

class GenericSyncColorMode(
    override val parent: ChoiceConfigurable<*>
) : GenericColorMode<Any?>("Sync") {

    override fun getColors(param: Any?): Pair<Color4b, Color4b> {
        return ModuleHud.PrimaryColor to ModuleHud.SecondaryColor
    }
}
class GenericRainbowColorMode(
    override val parent: ChoiceConfigurable<*>,
    private val alpha: Int = 50
) : GenericColorMode<Any?>("Rainbow") {

    override fun getColors(param: Any?) = rainbow().with(a = alpha) to rainbow().with(a = alpha)


    override fun getColor(param: Any?, angle: Int): Color4b {
        return rainbow(angle.toFloat()).with(a = alpha)
    }
}

class GenericCustomColorMode(
    override val parent: ChoiceConfigurable<*>,
    startColor: Color4b,
    endColor: Color4b
) : GenericColorMode<Any?>("Custom") {

    private val customStartColor by color("Start", startColor)
    private val customEndColor by color("End", endColor)

    override fun getColors(param: Any?) = customStartColor to customEndColor


    override fun getColor(param: Any?, angle: Int): Color4b {
        val progress = angle / 360f
        return Color4b(
            interpolate(customStartColor.r, customEndColor.r, progress),
            interpolate(customStartColor.g, customEndColor.g, progress),
            interpolate(customStartColor.b, customEndColor.b, progress),
            interpolate(customStartColor.a, customEndColor.a, progress)
        )
    }

    private fun interpolate(start: Int, end: Int, progress: Float): Int {
        return (start + (end - start) * progress).toInt().coerceIn(0..255)
    }
}

class MapColorMode(
    override val parent: ChoiceConfigurable<*>,
    private val alpha: Int = 100
) : GenericColorMode<Pair<BlockPos, BlockState>>("MapColor") {

    override fun getColors(param: Pair<BlockPos, BlockState>): Pair<Color4b, Color4b> {
        val (pos, state) = param
        val color = Color4b(state.getMapColor(world, pos).color).with(a = alpha)
        return color to color
    }

    // Override to provide single color directly
    override fun getColor(param: Pair<BlockPos, BlockState>): Color4b {
        val (pos, state) = param
        return Color4b(state.getMapColor(world, pos).color).with(a = alpha)
    }
}

class GenericEntityHealthColorMode(
    override val parent: ChoiceConfigurable<*>
) : GenericColorMode<LivingEntity>("Health") {

    override fun getColors(param: LivingEntity): Pair<Color4b, Color4b> {
        val color = calculateHealthColor(param)
        return color to color
    }

    // Override to provide single color directly
    override fun getColor(param: LivingEntity): Color4b {
        return calculateHealthColor(param)
    }

    private fun calculateHealthColor(entity: LivingEntity): Color4b {
        val maxHealth = entity.maxHealth
        val health = entity.getActualHealth().coerceAtMost(maxHealth)
        val healthPercentage = health / maxHealth

        val red = (255 * (1 - healthPercentage)).toInt().coerceIn(0..255)
        val green = (255 * healthPercentage).toInt().coerceIn(0..255)

        return Color4b(red, green, 0)
    }
}


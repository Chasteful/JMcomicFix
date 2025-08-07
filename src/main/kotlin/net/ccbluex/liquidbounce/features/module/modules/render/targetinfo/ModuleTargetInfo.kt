package net.ccbluex.liquidbounce.features.module.modules.render.targetinfo

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.targetinfo.mode.NovolineMode
import net.ccbluex.liquidbounce.render.GenericCustomColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b

object ModuleTargetInfo : ClientModule("TargetInfo", Category.RENDER, aliases = arrayOf("TargetHUD")) {

    val modes = choices("Mode", NovolineMode, arrayOf(NovolineMode))

    val colorModes = choices(this, "ColorMode", 2) {
        arrayOf(
            GenericCustomColorMode(it, Color4b.RED.with(a = 137), Color4b.RED.with(a = 233)),
            GenericStaticColorMode(it, Color4b.RED.with(a = 150)),
            GenericSyncColorMode(it)
        )
    }
    val xOffset by float("X-Offset", 450f, -1000f..1000f)
    val yOffset by float("Y-Offset", 250f, -1000f..1000f)


    val backgroundColor by color("Background", Color4b.DARK_GRAY.withAlpha(125))
    val borderColor by color("Border", Color4b.TRANSPARENT)
    val textColor by color("Name", Color4b.WHITE)

    override fun enable() {
        modes.activeChoice.enable()
    }

    override fun disable() {
        modes.activeChoice.disable()
    }

}

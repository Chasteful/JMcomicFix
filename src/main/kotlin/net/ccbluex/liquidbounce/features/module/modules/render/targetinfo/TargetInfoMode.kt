package net.ccbluex.liquidbounce.features.module.modules.render.targetinfo



import net.ccbluex.liquidbounce.config.types.nesting.Choice

abstract class TargetInfoMode (
    name: String
) : Choice(name) {
    override val parent
        get() = ModuleTargetInfo.modes
}

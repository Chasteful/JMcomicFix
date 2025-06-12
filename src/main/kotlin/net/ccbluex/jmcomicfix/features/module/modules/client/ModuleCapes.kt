// ModuleCapes.kt
package net.ccbluex.jmcomicfix.features.module.modules.client

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.ccbluex.liquidbounce.features.cosmetic.CapeCosmeticsManager
import net.minecraft.util.Identifier
import net.ccbluex.liquidbounce.LiquidBounce

object ModuleCapes : ClientModule("Capes", Category.CLIENT) {

    /**
     * 枚举里只保存文件名，实际注册贴图时会利用这个 fileName：
     */
    @Suppress("UNUSED")
    enum class CapeMode(
        override val choiceName: String,
        val fileName: String
    ) : NamedChoice {
        Astolfo    ("Astolfo",     "Astolfo"),
        Diana      ("Diana",       "Diana"),
        FDP        ("FDP",         "FDP"),
        Tenacity   ("Tenacity",    "Tenacity"),
        LiquidBounce("LiquidBounce", "LiquidBounce"),
        JMcomicFix  ("JMcomicFix","JMcomicFix"),
        Novoline   ("Novoline",    "Novoline"),
        Opal       ("Opal",        "Opal"),
        PowerX     ("PowerX",      "PowerX"),
        Rise       ("Rise",        "Rise"),
        VapeV4     ("VapeV4",      "Vape_V4"),
        VapeLite   ("VapeLite",    "Vape_Lite"),

    }


    val capeMode by enumChoice("CapeMode", CapeMode.JMcomicFix)

    private var localCapeIdentifier: Identifier? = null
    private var localCapeModeSelected: CapeMode? = null


    fun getLocalCapeTextureId(): Identifier {
        val mode = capeMode

        if (localCapeIdentifier == null || localCapeModeSelected != mode) {

            localCapeIdentifier?.let { mc.textureManager.destroyTexture(it) }

            val newId = "image/capes/${mode.fileName}.png"
                .registerAsDynamicImageFromClientResources()
            localCapeIdentifier = newId
            localCapeModeSelected = mode

            LiquidBounce.logger.info("已注册本地玩家新披风：${mode.fileName} → $newId")
        }
        return localCapeIdentifier!!
    }

    fun getCapeTextureId(): Identifier = getLocalCapeTextureId()

    override fun enable() {
        LiquidBounce.logger.info("ModuleCapes 已开启")

        localCapeIdentifier?.let { mc.textureManager.destroyTexture(it) }
        localCapeIdentifier = null
        localCapeModeSelected = null
    }

    override fun disable() {
        LiquidBounce.logger.info("ModuleCapes 已关闭，开始清理所有披风贴图")

        localCapeIdentifier?.let { mc.textureManager.destroyTexture(it) }
        localCapeIdentifier = null
        localCapeModeSelected = null

        CapeCosmeticsManager.clearAllCachedCapes()
    }
}

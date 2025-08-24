package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

@Suppress("unused")
object ModuleAutoVoid  : ClientModule("AutoVoid ", Category.FUN, aliases =  arrayOf("Depression")) {

 private val message by boolean("Message",false)

}

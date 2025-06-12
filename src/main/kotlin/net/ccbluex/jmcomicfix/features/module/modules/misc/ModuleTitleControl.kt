package net.ccbluex.jmcomicfix.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.event.events.OverlayTitleEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.text.Text


/**
 * ModuleTitleControl：拦截 OverlayTitleEvent，将不符合条件的 Title/Subtitle 直接置空
 */
object ModuleTitleControl : ClientModule("TitleControl", Category.MISC) {

    private sealed class FilterChoice(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*> get() = filterMode

        abstract fun shouldDisplay(title: String?, subtitle: String?): Boolean

        object BLACKLIST : FilterChoice("Blacklist") {
            override fun shouldDisplay(title: String?, subtitle: String?) =
                !hasAnyKeyword(title) && !hasAnyKeyword(subtitle)
        }

        object WHITELIST : FilterChoice("Whitelist") {
            override fun shouldDisplay(title: String?, subtitle: String?) =
                hasAnyKeyword(title) || hasAnyKeyword(subtitle)
        }

        protected fun hasAnyKeyword(text: String?): Boolean {
            if (text.isNullOrEmpty()) return false
            return filteredKeywords.any { keyword ->
                text.contains(keyword, ignoreCase = true)
            }
        }
    }

    private val filterMode = choices("FilterMode",
        FilterChoice.BLACKLIST, arrayOf(FilterChoice.BLACKLIST, FilterChoice.WHITELIST))
    private val keywords by textArray("Keywords", mutableListOf("example"))


    private val filteredKeywords get() = keywords
    fun Text.toFlatString(): String {
        val builder = StringBuilder()
        fun extract(text: Text?) {
            if (text == null) return
            builder.append(text.string)
            text.siblings.forEach { extract(it) }
        }
        extract(this)
        return builder.toString()
    }
    init {
        handler<OverlayTitleEvent>(priority = -1000) { event ->

            val titleText = event.title.toFlatString().takeIf { it.isNotEmpty() }
            val subtitleText = event.subtitle.toFlatString().takeIf { it.isNotEmpty() }

            val shouldDisplay = filterMode.activeChoice.shouldDisplay(titleText, subtitleText)

            if (!shouldDisplay) {
                event.title = Text.empty()
                event.subtitle = Text.empty()
            }
        }
    }
}

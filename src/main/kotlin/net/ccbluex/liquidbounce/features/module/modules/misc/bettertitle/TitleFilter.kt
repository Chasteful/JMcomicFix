package net.ccbluex.liquidbounce.features.module.modules.misc.bettertitle

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.TitleEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleBetterTitle
import net.ccbluex.liquidbounce.features.module.modules.misc.TitleType
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.getNextCount
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object TitleFilter : ToggleableConfigurable(ModuleBetterTitle, "TitleFilter", true) {

    private var regexFilters = emptySet<Regex>()
    private val stack by boolean("StackTitles", false)
    private val filters by textList("Filters", mutableListOf()).onChanged {
        compileFilters()
    }

    private fun compileFilters() {
        regexFilters = filters.mapTo(HashSet(filters.size, 1.0f), ::Regex)
    }

    private inline fun <reified E : TitleEvent.TextContent> filterHandler(
        type: TitleType
    ) = handler<E> { event ->
        val string = event.text
            ?.string
            ?.stripMinecraftColorCodes()
            ?.takeUnless(String::isBlank)
            ?: return@handler

        if (regexFilters.isNotEmpty() && regexFilters.any { it.matches(string) }) {
            event.cancelEvent()
            return@handler
        }

        if (stack) {
            event.cancelEvent()

            val id = "$string-external"
            val literalText = Text.literal(string)

            val newCount = getNextCount(id)
            if (newCount > 1) {
                literalText.append(" ").append(Text.literal("[$newCount]").formatted(Formatting.GRAY))
            }

            val data = MessageMetadata(prefix = false, id = id, remove = true, count = newCount)
            chat(
                texts = arrayOf(
                    Text.literal(type.choiceName).formatted(Formatting.AQUA),
                    Text.literal(": ").formatted(Formatting.RESET),
                    literalText
                ),
                metadata = data
            )

            type.setText(literalText)
        }
    }

    @Suppress("unused")
    val titleFilterHandler = filterHandler<TitleEvent.Title>(TitleType.TITLE)

    @Suppress("unused")
    val subtitleFilterHandler = filterHandler<TitleEvent.Subtitle>(TitleType.SUBTITLE)
}

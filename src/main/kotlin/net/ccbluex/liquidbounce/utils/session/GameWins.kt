package net.ccbluex.liquidbounce.utils.session

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinHudAccessor
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.network.chat.Component;
import java.util.concurrent.TimeUnit

object GameWins : EventListener {
    private var lastWinDetectionTime = 0L
    private val cooldownMillis = TimeUnit.SECONDS.toMillis(10)

    var victoryCount = 0
        private set

    init {
        handler<GameTickEvent> {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastWinDetectionTime < cooldownMillis) return@handler


            val hud = mc.gui as? MixinHudAccessor ?: return@handler

            val title = hud.getTitle()
            val subtitle = hud.getSubtitle()

            if (checkWinCondition(title) || checkSubtitleWinCondition(subtitle)) {
                lastWinDetectionTime = currentTime
                victoryCount++
            }
        }

        handler<ChatReceiveEvent> { event ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastWinDetectionTime < cooldownMillis) return@handler

            if (checkChatWinCondition(event.textData)) {
                lastWinDetectionTime = currentTime
                victoryCount++
            }
        }
    }

    private fun checkWinCondition(text: Component?): Boolean {
        if (text == null || text.string.isEmpty()) return false
        return winKeywordsTitle.any { text.string.contains(it, ignoreCase = true) }
    }

    private fun checkSubtitleWinCondition(text: Component?): Boolean {
        if (text == null || text.string.isEmpty()) return false
        return winKeywordsSubtitle.any { text.string.contains(it, ignoreCase = true) }
    }

    private fun checkChatWinCondition(text: Component): Boolean {
        val message = text.string
        if (message.isEmpty()) return false
        return winKeywordsChatMessage.any { message.contains(it, ignoreCase = true) }
    }

    private val winKeywordsTitle = listOf(
        "胜利","Victory","你赢了",
    )

    private val winKeywordsSubtitle = listOf(
        "恭喜你赢下了比赛", "你是最后的站立者",
        "you win", "Good Game",
        "victoire", "gagné",
        "gewonnen",
        "vittoria",
        "¡ganaste!", "victoria",
        "победа",
        "승리",
        "勝った", "勝利"
    )

    private val winKeywordsChatMessage = listOf(
        "YOU WON!!!"
    )
}

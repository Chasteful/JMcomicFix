package net.ccbluex.liquidbounce.utils.session

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.world.entity.player.Player

object PlayerDeadEventListener : EventListener {

    private var wasAliveLastTick = true
    private var localPlayerDeathCounter = 0

    val deathCount: Int
        get() = localPlayerDeathCounter

    init {
        tickHandler {
            mc.player?.let { player ->
                updateDeathCount(player)
            }
        }
    }

    private fun updateDeathCount(player: Player) {
        if (player != mc.player) return

        val isNowDead = player.isRemoved || !player.isAlive
        if (wasAliveLastTick && isNowDead) {
            localPlayerDeathCounter++
        }
        wasAliveLastTick = !isNowDead
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet !is ClientboundSystemChatPacket) return@handler

        val message = packet.content.string
        val playerName = mc.player?.name?.string ?: return@handler

        val combinedPattern = Regex(
            listOf(
                //Generic
                "you died!",
                "你死了！",
                "You were killed by .+",
                "你被 .+ 杀死",

                //Minecraft Vanilla English
                "\\b${Regex.escape(playerName)}\\b was slain by .+",
                "\\b${Regex.escape(playerName)}\\b was shot by .+",
                "\\b${Regex.escape(playerName)}\\b fell out of the world\b.",
                "\\b${Regex.escape(playerName)}\\b fell from a high place\b.",
                "\\b${Regex.escape(playerName)}\\b burned to death\b.",
                "\\b${Regex.escape(playerName)}\\b tried to swim in lava\b.",
                "\\b${Regex.escape(playerName)}\\b drowned\b.",
                "\\b${Regex.escape(playerName)}\\b suffocated in a wall\b.",
                "\\b${Regex.escape(playerName)}\\b was pricked to death\b.",
                "\\b${Regex.escape(playerName)}\\b starved to death\b.",
                "\\b${Regex.escape(playerName)}\\b was blown up by .+",
                "\\b${Regex.escape(playerName)}\\b was killed by magic\b.",
                "\\b${Regex.escape(playerName)}\\b was killed by .+",
                "\\b${Regex.escape(playerName)}\\b hit the ground too hard\b.",
                "\\b${Regex.escape(playerName)}\\b was knocked into the void by .+",

                "\\b${Regex.escape(playerName)}\\b 被 .+ 杀死",
                "\\b${Regex.escape(playerName)}\\b 被 .+ 射杀",
                "\\b${Regex.escape(playerName)}\\b 掉出了世界\b.",
                "\\b${Regex.escape(playerName)}\\b 从高处摔落\b.",
                "\\b${Regex.escape(playerName)}\\b 烧死了\b.",
                "\\b${Regex.escape(playerName)}\\b 试图在熔岩中游泳\b.",
                "\\b${Regex.escape(playerName)}\\b 溺死了\b.",
                "\\b${Regex.escape(playerName)}\\b 在墙里窒息\b.",
                "\\b${Regex.escape(playerName)}\\b 被刺死了\b.",
                "\\b${Regex.escape(playerName)}\\b 饿死了\b.",
                "\\b${Regex.escape(playerName)}\\b 被 .+ 炸死",
                "\\b${Regex.escape(playerName)}\\b 被魔法杀死\b.",
                "\\b${Regex.escape(playerName)}\\b 摔得太重了\b.",
                "\\b${Regex.escape(playerName)}\\b 被 .+ 推入虚空",

                "\\b${Regex.escape(playerName)}\\b was killed by .+",
                "\\b${Regex.escape(playerName)}\\b was knocked into the void by .+",
                "\\b${Regex.escape(playerName)}\\b fell into the void.",
                "\\b${Regex.escape(playerName)}\\b was slain by <mob>",
                "\\b${Regex.escape(playerName)}\\b burned to death while fighting .+",
                "\\b${Regex.escape(playerName)}\\b drowned whilst trying to escape .+",
                "\\b${Regex.escape(playerName)}\\b hit the ground too hard whilst trying to escape .+",
                "\\b${Regex.escape(playerName)}\\b was killed by magic whilst trying to escape .+",
                "\\b${Regex.escape(playerName)}\\b was deleted by .+",
                "\\b${Regex.escape(playerName)}\\b was injected with malware by .+",
                "\\b${Regex.escape(playerName)}\\b was blown up by .+",
                "\\b${Regex.escape(playerName)}\\b died in close combat to .+",
                "\\b${Regex.escape(playerName)}\\b was finally killed by .+",

                "\\b${Regex.escape(playerName)}\\b 被 .+? 击杀",
                "\\b${Regex.escape(playerName)}\\b 被 .+ 推入虚空",
                "\\b${Regex.escape(playerName)}\\b 被 <mob> 杀死",
                "\\b${Regex.escape(playerName)}\\b 在与 .+ 战斗时烧死了",
                "\\b${Regex.escape(playerName)}\\b 试图逃离 .+ 时溺死了",
                "\\b${Regex.escape(playerName)}\\b 试图逃离 .+ 时摔得太重了",
                "\\b${Regex.escape(playerName)}\\b 试图逃离 .+ 时被魔法杀死",
                "\\b${Regex.escape(playerName)}\\b 被 .+ 删除",
                "\\b${Regex.escape(playerName)}\\b 被 .+? 赶出了联盟",
                "\\b${Regex.escape(playerName)}\\b 被 .+ 注入恶意软件",
                "\\b${Regex.escape(playerName)}\\b 被 .+ 炸飞",
                "\\b${Regex.escape(playerName)}\\b 在近身战斗中死于 .+",
                "\\b${Regex.escape(playerName)}\\b 被 .+ 最终杀死",
                "\\b${Regex.escape(playerName)}\\b 失足跌入虚空",

                //KKCraft
                "\\b${Regex.escape(playerName)}脚滑掉落虚空",
                "\\b${Regex.escape(playerName)}被击杀",

                // BMW
                "\\b${Regex.escape(playerName)}\\b 被 .+? 击败.*",
                "\\b${Regex.escape(playerName)}\\b 被炸成了粉尘, 最终还是被 .+? 击败.*",
                "\\b${Regex.escape(playerName)}\\b 消逝了, 最终还是被 .+? 击败.*",
                "\\b${Regex.escape(playerName)}\\b 被架在了烧烤架上, 熟透了, 最终还是被 .+? 击败.*",
                "\\b${Regex.escape(playerName)}\\b 跑得很快, 但是他还是摔了一跤, 最终被 .+? 击败.*",
                "\\b${Regex.escape(playerName)}\\b 被 .+? 用弓箭射穿了.*",
                "\\b${Regex.escape(playerName)}\\b 被重压地无法呼吸, 最终还是被 .+? 击败.*",

                // Existing ones
                "\\b${Regex.escape(playerName)}\\b died.",
                "\\b${Regex.escape(playerName)}\\b has died.",
                "\\b${Regex.escape(playerName)}\\b was killed by",
                "\\b${Regex.escape(playerName)}\\b 被击杀",

            ).joinToString("|"),
            RegexOption.IGNORE_CASE
        )

        if (combinedPattern.containsMatchIn(message)) {
            localPlayerDeathCounter++
        }
    }

}

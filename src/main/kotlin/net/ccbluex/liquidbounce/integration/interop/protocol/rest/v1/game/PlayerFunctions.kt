/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import net.ccbluex.jmcomicfix.features.module.modules.render.ModuleKillEffects
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock.hideShieldSlot
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock.shouldHideOffhand
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.netherPosition
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.scoreboard.Scoreboard
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.ScoreboardEntry
import net.minecraft.scoreboard.Team
import net.minecraft.scoreboard.number.NumberFormat
import net.minecraft.scoreboard.number.StyledNumberFormat
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameMode
import kotlin.math.min

// GET /api/v1/client/player
@Suppress("UNUSED_PARAMETER")
fun getPlayerData(requestObject: RequestObject) = httpOk(interopGson.toJsonTree(PlayerData.fromPlayer(player)))

// GET /api/v1/client/player/inventory
@Suppress("UNUSED_PARAMETER")
fun getPlayerInventory(requestObject: RequestObject) =
    httpOk(interopGson.toJsonTree(PlayerInventoryData.fromPlayer(player)))

// GET /api/v1/client/crosshair
@Suppress("UNUSED_PARAMETER")
fun getCrosshairData(requestObject: RequestObject) = httpOk(interopGson.toJsonTree(mc.crosshairTarget))

data class PlayerData(
    val username: String,
    val uuid: String,
    val dimension: Identifier,
    val position: Vec3d,
    val pitch: Float,
    val yaw: Float,
    val netherPosition: Vec3d,
    val blockPosition: BlockPos,
    val velocity: Vec3d,
    val selectedSlot: Int,
    val gameMode: GameMode = GameMode.DEFAULT,
    val health: Float,
    val actualHealth: Float,
    val maxHealth: Float,
    val absorption: Float,
    val armor: Int,
    val food: Int,
    val air: Int,
    val maxAir: Int,
    val experienceLevel: Int,
    val experienceProgress: Float,
    val ping: Int,
    val serverAddress: String,
    val effects: List<StatusEffectInstance>,
    val mainHandStack: ItemStack,
    val offHandStack: ItemStack,
    val armorItems: List<ItemStack> = emptyList(),
    val scoreboard: ScoreboardData? = null,
    val killsCount: Int,
    val deathCount: Int,
    val isDead: Boolean,
    val isEating: Boolean,
    val eatingStartTime: Long,
    val eatingMaxDuration:  Int,
    val playTime: Long,

) {

    companion object {

        fun fromPlayer(player: PlayerEntity) = PlayerData(

            player.nameForScoreboard,
            player.uuidAsString,
            player.world.registryKey.value,
            player.pos,
            player.pitch,
            player.yaw,
            player.netherPosition,
            player.blockPos,
            player.velocity,
            player.inventory.selectedSlot,
            if (mc.player == player) interaction.currentGameMode else GameMode.DEFAULT,
            player.health.fixNaN(),
            player.getActualHealth().fixNaN(),
            player.maxHealth.fixNaN(),
            player.absorptionAmount.fixNaN(),
            player.armor.coerceAtMost(20),
            min(player.hungerManager.foodLevel, 20),
            player.air,
            player.maxAir,
            player.experienceLevel,
            player.experienceProgress.fixNaN(),
            player.ping,
            mc.currentServerEntry?.address ?: "",
            player.statusEffects.toList(),
            player.mainHandStack,
            if (shouldHideOffhand(player = player) && hideShieldSlot) ItemStack.EMPTY else player.offHandStack,
            player.armorItems.toList(),
            if (mc.player == player) ScoreboardData.fromScoreboard(player.scoreboard) else null,
            ModuleKillEffects.killsCount,
            updateDeathCount(player),
            player.isDead,
            ModuleAutoBuff.isEating,
            ModuleAutoBuff.eatingStartTime,
            ModuleAutoBuff.eatingMaxDuration,
            PlayTimeTracker.getPlayTime(),


        )
        private var wasAliveLastTick = true
        private var localPlayerDeathCounter = 0
        private fun updateDeathCount(player: PlayerEntity): Int {
            if (player != mc.player) return localPlayerDeathCounter

            val isNowDead = player.isRemoved || !player.isAlive
            if (wasAliveLastTick && isNowDead) {
                localPlayerDeathCounter++
            }
            wasAliveLastTick = !isNowDead

            return localPlayerDeathCounter
        }
    }

}

data class PlayerInventoryData(
    val armor: List<ItemStack>,
    val main: List<ItemStack>,
    val crafting: List<ItemStack>
) {

    companion object {
        fun fromPlayer(player: PlayerEntity) = PlayerInventoryData(
            armor = player.inventory.armor.map(ItemStack::copy),
            main = player.inventory.main.map(ItemStack::copy),
            crafting = player.playerScreenHandler.craftingInput.heldStacks.map(ItemStack::copy)
        )
    }

    override fun hashCode(): Int {
        var result = armor.hashCode()
        result = 31 * result + main.hashCode()
        result = 31 * result + crafting.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerInventoryData

        if (armor.size != other.armor.size) return false
        if (main.size != other.main.size) return false
        if (crafting.size != other.crafting.size) return false

        for (i in armor.indices) {
            if (!ItemStack.areEqual(armor[i], other.armor[i])) {
                return false
            }
        }

        for (i in main.indices) {
            if (!ItemStack.areEqual(main[i], other.main[i])) {
                return false
            }
        }

        for (i in crafting.indices) {
            if (!ItemStack.areEqual(crafting[i], other.crafting[i])) {
                return false
            }
        }

        return true
    }

}

data class SidebarEntry(val name: Text, val score: Text)

data class ScoreboardData(val header: Text, val entries: Array<SidebarEntry?>) {

    companion object {

        /**
         * Creates a [ScoreboardData] from the [player]'s scoreboard
         *
         * Taken from the Minecraft source code
         */
        fun fromScoreboard(scoreboard: Scoreboard?): ScoreboardData? {
            if (scoreboard == null) return null

            val team = scoreboard.getScoreHolderTeam(player.nameForScoreboard)

            val objective = team?.let {
                ScoreboardDisplaySlot.fromFormatting(team.color)?.let { scoreboard.getObjectiveForSlot(it) }
            } ?: scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) ?: return null

            val objectiveScoreboard: Scoreboard = objective.scoreboard
            val numberFormat: NumberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED)

            val entryComparator = Comparator
                .comparing { scoreboardEntry: ScoreboardEntry -> scoreboardEntry.value() }
                .reversed()
                .thenComparing({ it.owner() }, String.CASE_INSENSITIVE_ORDER)

            val sidebarEntries = objectiveScoreboard.getScoreboardEntries(objective)
                .stream()
                .filter { score: ScoreboardEntry -> !score.hidden() }
                .sorted(entryComparator)
                .limit(15L)
                .map { scoreboardEntry: ScoreboardEntry ->
                    val team = objectiveScoreboard.getScoreHolderTeam(scoreboardEntry.owner())
                    val entryName = scoreboardEntry.name()
                    val entryWithDecoration: Text = Team.decorateName(team, entryName)
                    val entryValue: Text = scoreboardEntry.formatted(numberFormat)

                    SidebarEntry(entryWithDecoration.sanitizeForeignInput(), entryValue.sanitizeForeignInput())
                }
                .toArray { arrayOfNulls<SidebarEntry>(it) }

            return ScoreboardData(objective.displayName.sanitizeForeignInput(), sidebarEntries)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScoreboardData

        if (header != other.header) return false
        if (!entries.contentEquals(other.entries)) return false

        return true
    }



    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + entries.contentHashCode()
        return result
    }

}

object PlayTimeTracker {
    private val playTimeMap = mutableMapOf<String, Long>()
    private var lastServer: String? = null
    private var lastUpdateTime: Long = System.currentTimeMillis()
    private var leftoverMs: Long = 0

    fun update() {
        val address = mc.currentServerEntry?.address ?: return
        val now = System.currentTimeMillis()
        val deltaMs = now - lastUpdateTime + leftoverMs

        if (deltaMs < 1000) {
            leftoverMs = deltaMs
            return
        }

        val deltaSeconds = deltaMs / 1000
        leftoverMs = deltaMs % 1000

        playTimeMap[address] = playTimeMap.getOrDefault(address, 0L) + deltaSeconds
        lastUpdateTime = now
        lastServer = address
    }

    fun getPlayTime(): Long {
        val address = mc.currentServerEntry?.address ?: return 0
        return playTimeMap[address] ?: 0
    }
}

/**
 * GSON is not happy with NaN values, so we fix them to be 0.
 */
private fun Float.fixNaN() = if (isNaN()) 0f else this

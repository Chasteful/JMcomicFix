package net.ccbluex.liquidbounce.utils.session

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket
import net.minecraft.util.hit.HitResult
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt

object KilledTarget : EventListener {
    private val attackedEntities = ConcurrentHashMap<LivingEntity, AttackData>()
    private val killedEntities = LinkedHashSet<LivingEntity>()
    private var _killsCount = 0
    private var isInLobby = false

    data class AttackData(
        var lastAttackTime: Long,
        var lastVelocity: Double,
        var lastHurtTime: Int,
        var confirmedDamage: Boolean = false
    )

    private val lobbyKeywords = listOf(
        // Core lobby/hub/spawn terms
        "hub", "lobby", "plaza", "square",
        // Localized lobby terms
        "大厅", // Chinese (Hall/Lobby)
        "ロビー", // Japanese (Lobby)
        "로비", // Korean (Lobby)
        "sala", // Spanish (Hall/Lobby)
        "salon", // French (Hall/Lobby)
        "lounge", // English/French (Lounge)
        "foyer", // French/English (Foyer)
        "vestibulo", // Spanish (Vestibule)
        "eingang", // German (Entrance)
        // Login/registration terms
        "login", "register", "登记", "ログイン",
        "anmeldung", "registro", "connexion", "로그인",
        // Social media and community terms
        "discord", "twitter",
        "ディスコード", // Japanese (Discord)
        "ツイッター", // Japanese (Twitter)
        "트위터", // Korean (Twitter)
    )
    @JvmStatic
    val killsCount: Int
        get() = _killsCount

    fun getKilledEntities(): Set<LivingEntity> {
        synchronized(killedEntities) {
            val copy = killedEntities.toSet()
            killedEntities.clear()
            return copy
        }
    }

    val attackHandler = handler<AttackEntityEvent> { event ->
        val entity = event.entity
        val mc = MinecraftClient.getInstance()

        // Skip if in a lobby or non-PvP area
        if (isInLobby) {
            return@handler
        }

        if (entity == mc.player || !entity.isAlive || entity !is LivingEntity || !entity.shouldBeAttacked()) {
            return@handler
        }

        val hitResult = mc.crosshairTarget
        if (hitResult == null || hitResult.type != HitResult.Type.ENTITY) {
            return@handler
        }

        // Store attack data with initial velocity and hurt time
        val currentVelocity = entity.velocity.horizontalLength()
        attackedEntities[entity] = AttackData(
            lastAttackTime = System.currentTimeMillis(),
            lastVelocity = currentVelocity,
            lastHurtTime = entity.hurtTime
        )
    }

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        val now = System.currentTimeMillis()

        // Detect lobby via scoreboard
        if (packet is ScoreboardObjectiveUpdateS2CPacket) {
            val objectiveName = packet.name.lowercase()
            val displayName = packet.displayName?.string?.lowercase() ?: ""
            isInLobby = lobbyKeywords.any { keyword ->
                objectiveName.contains(keyword) || displayName.contains(keyword)
            }
        }

        // Skip processing if in a lobby
        if (isInLobby) {
            attackedEntities.clear()
            return@handler
        }

        // Detect velocity changes (knockback)
        if (packet is EntityVelocityUpdateS2CPacket) {
            attackedEntities.entries.forEach { (entity, data) ->
                if (entity.id == packet.entityId && now - data.lastAttackTime < 1000L) {
                    // Calculate velocity magnitude from packet's X and Z components
                    val newVelocity = sqrt((
                        packet.velocityX * packet.velocityX + packet.velocityZ * packet.velocityZ).toDouble()) / 8000.0
                    if (abs(newVelocity - data.lastVelocity) > 0.05) { // Detect significant velocity change
                        data.confirmedDamage = true
                        data.lastVelocity = newVelocity
                    }
                }
            }
        }

        // Detect hurt sound (fallback, use cautiously)
        if (packet is PlaySoundS2CPacket && packet.sound.value().id.path.contains("entity.generic.hurt")) {
            attackedEntities.entries.forEach { (entity, data) ->
                if (now - data.lastAttackTime < 1000L &&
                    abs(packet.x - entity.x) < 1.0 &&
                    abs(packet.z - entity.z) < 1.0
                ) {
                    data.confirmedDamage = true
                }
            }
        }
    }


    fun tick() {
        val now = System.currentTimeMillis()
        val iterator = attackedEntities.entries.iterator()

        while (iterator.hasNext()) {
            val (entity, data) = iterator.next()

            if (entity.isRemoved || !entity.isAlive) {
                // Confirm kill if damage was detected or entity died shortly after attack
                if (data.confirmedDamage || now - data.lastAttackTime < 1000L) {
                    _killsCount++
                    synchronized(killedEntities) {
                        killedEntities.add(entity)
                    }
                }
                iterator.remove()
            } else if (now - data.lastAttackTime > 5000L) {
                // Remove stale entries
                iterator.remove()
            } else if (entity.hurtTime > data.lastHurtTime) {
                // Detect new hurt animation
                data.confirmedDamage = true
                data.lastHurtTime = entity.hurtTime
                data.lastAttackTime = now // Refresh timestamp
            }
        }
    }
}

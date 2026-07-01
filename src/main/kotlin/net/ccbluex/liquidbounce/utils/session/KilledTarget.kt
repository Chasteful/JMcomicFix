package net.ccbluex.liquidbounce.utils.session

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt

object KilledTarget : EventListener {
    private val attackedEntities = ConcurrentHashMap<LivingEntity, AttackData>()

    val effectsRenderQueue = LinkedHashSet<LivingEntity>()

    private var _killsCount = 0
    private var isInLobby = false

    data class AttackData(
        var lastAttackTime: Long,
        var lastVelocity: Double,
        var lastHurtTime: Int,
        val attackerId: Int,
        var confirmedDamage: Boolean = false
    )

    init {
        handler<GameTickEvent> {
            val now = System.currentTimeMillis()
            val iterator = attackedEntities.entries.iterator()

            while (iterator.hasNext()) {
                val (entity, data) = iterator.next()

                if (!entity.isAlive || entity.isDeadOrDying || entity.health <= 0f) {
                    if ((data.confirmedDamage || now - data.lastAttackTime < 1000L)
                        && data.attackerId == mc.player?.id
                    ) {
                        _killsCount++

                        synchronized(effectsRenderQueue) {
                            effectsRenderQueue.add(entity)
                        }
                    }
                    iterator.remove()
                } else if (now - data.lastAttackTime > 5000L) {
                    iterator.remove()
                } else if (entity.hurtTime > data.lastHurtTime) {
                    data.confirmedDamage = true
                    data.lastHurtTime = entity.hurtTime
                    data.lastAttackTime = now
                }
            }
        }
    }

    private val lobbyKeywords = listOf(
        "hub", "lobby", "plaza", "square", "大厅", "ロビー", "로비", "sala", "salon",
        "lounge", "foyer", "vestibulo", "eingang", "login", "register", "登记",
        "ログイン", "anmeldung", "registro", "connexion", "로그인", "discord", "twitter",
        "ディスコード", "ツイッター", "트위터"
    )

    @JvmStatic
    val killsCount: Int
        get() = _killsCount

    fun getKilledEntitiesForRender(): Set<LivingEntity> {
        synchronized(effectsRenderQueue) {
            val copy = effectsRenderQueue.toSet()
            effectsRenderQueue.clear()
            return copy
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        val entity = event.entity as LivingEntity

        if (isInLobby) return@handler

        if (entity == mc.player || !entity.isAlive || entity.type != EntityTypes.PLAYER || !entity.shouldBeAttacked()) {
            return@handler
        }

        val currentVelocity = entity.deltaMovement.horizontalDistance()
        attackedEntities[entity] = AttackData(
            lastAttackTime = System.currentTimeMillis(),
            lastVelocity = currentVelocity,
            lastHurtTime = entity.hurtTime,
            attackerId = mc.player!!.id
        )
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        val now = System.currentTimeMillis()

        if (packet is ClientboundSetObjectivePacket) {
            val objectiveName = packet.objectiveName.lowercase()
            val displayName = packet.displayName.string.lowercase()

            val isLobbyObjective = lobbyKeywords.any { keyword ->
                objectiveName.contains(keyword) || displayName.contains(keyword)
            }

            if (isLobbyObjective) {
                isInLobby = true
            }
        }

        if (packet is ClientboundSetScorePacket) {
            val objectiveName = packet.objectiveName.lowercase()
            val ownerName = packet.owner.lowercase()

            val isLobbyScore = lobbyKeywords.any { keyword ->
                objectiveName.contains(keyword) || ownerName.contains(keyword)
            }

            if (isLobbyScore) {
                isInLobby = true
            }
        }

        if (isInLobby) {
            attackedEntities.clear()
            synchronized(effectsRenderQueue) { effectsRenderQueue.clear() }
            return@handler
        }

        if (packet is ClientboundRespawnPacket) {
            isInLobby = false
            attackedEntities.clear()
            synchronized(effectsRenderQueue) { effectsRenderQueue.clear() }
            return@handler
        }

        if (packet is ClientboundEntityEventPacket) {
            if (packet.eventId.toInt() == 2) {
                val entityId = mc.level?.let {
                    packet.getEntity(it)
                }?.id
                attackedEntities.entries.forEach { (entity, data) ->
                    if (entity.id == entityId && now - data.lastAttackTime < 1000L) {
                        data.confirmedDamage = true
                    }
                }
            }
        }
    }
}

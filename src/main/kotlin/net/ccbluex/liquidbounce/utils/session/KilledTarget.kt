package net.ccbluex.liquidbounce.utils.session

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.entity.LivingEntity
import java.util.concurrent.ConcurrentHashMap

object KilledTarget : EventListener {
    private val attackedEntities = ConcurrentHashMap<LivingEntity, Pair<Long, Float>>()
    private val killedEntities = LinkedHashSet<LivingEntity>() // Track killed entities
    private var _killsCount = 0

    @JvmStatic
    val killsCount: Int
        get() = _killsCount


    fun getKilledEntities(): Set<LivingEntity> {
        synchronized(killedEntities) {
            val copy = killedEntities.toSet() // Create a copy
            killedEntities.clear() // Clear the original
            return copy
        }
    }

    val attackHandler = handler<AttackEntityEvent> { event ->
        val entity = event.entity
        if (!entity.isAlive) {
            return@handler
        }
        if (entity is LivingEntity && entity.shouldBeAttacked()) {
            attackedEntities[entity] = System.currentTimeMillis() to entity.health
        }
    }

    fun tick() {
        val now = System.currentTimeMillis()
        val iterator = attackedEntities.entries.iterator()

        while (iterator.hasNext()) {
            val (entity, data) = iterator.next()
            val (timestamp, oldHealth) = data

            if (entity.isRemoved || !entity.isAlive) {
                val newHealth = entity.health

                if (newHealth < oldHealth || !entity.isAlive) {
                    _killsCount++
                    synchronized(killedEntities) {
                        killedEntities.add(entity)
                    }
                }
                iterator.remove()
            } else if (now - timestamp > 3000L) {
                iterator.remove()
            }
        }
    }
}

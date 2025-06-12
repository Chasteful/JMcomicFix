package net.ccbluex.jmcomicfix.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LightningEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.sound.SoundEvents


object ModuleKillEffects : ClientModule("KillEffects", Category.RENDER) {
    private val lightning by boolean("Lightning", true)
    private val mobs by boolean("Mobs", false)
    private val volume by float("Volume", 1f, 0f..1f)
    private val renderEntities = mutableMapOf<Entity, Long>()

    var killsCount: Int = 0
        private set

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            val now = System.currentTimeMillis()
            mc.world?.entities?.forEach { entity ->
                if (entity == mc.player || renderEntities.containsKey(entity)) return@forEach
                if (!mobs && entity !is PlayerEntity) return@forEach
                if (entity is LivingEntity && (!entity.isAlive || entity.health <= 0)) {
                    renderEntities[entity] = now
                    killsCount++
                    onKillEffect(entity)
                }
            }

            renderEntities.entries.removeIf { (e, t) -> now - t > 3000 }
        }
    }


    private fun onKillEffect(entity: Entity) {
        if (lightning) {
            val world = mc.world ?: return
            val bolt = LightningEntity(EntityType.LIGHTNING_BOLT, world)
            bolt.refreshPositionAfterTeleport(entity.pos)
            bolt.setCosmetic(true)
            world.spawnEntity(bolt)

            mc.soundManager.play(
                PositionedSoundInstance.master(
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                    volume,
                    1f
                )
            )
        }
    }
}

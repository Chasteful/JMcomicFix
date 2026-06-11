package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.session.KilledTarget
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.particles.DustColorTransitionOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.ShriekParticleOption
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.Blocks
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ModuleKillEffects : ClientModule("KillEffects", ModuleCategories.RENDER) {
    private val effects by multiEnumChoice(
        "Effects",
        Effect.BLOOD,
        canBeNone = false
    )
    private val volume by float("Volume", 1f, 0f..1f)
    private val particleCount by int("Particle Count", 20, 5..100)
    private val renderEntities = mutableMapOf<Entity, Long>()

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val now = System.currentTimeMillis()

        KilledTarget.getKilledEntitiesForRender().forEach { entity ->
            if (entity == mc.player) return@forEach

            if (entity.type == EntityType.PLAYER) {
                if (!renderEntities.containsKey(entity)) {
                    renderEntities[entity] = now
                    onKillEffect(entity)
                }
            }
        }

        val effectIterator = renderEntities.entries.iterator()
        while (effectIterator.hasNext()) {
            val (_, time) = effectIterator.next()
            if (now - time > 3000L) effectIterator.remove()
        }
    }

    @Suppress("CognitiveComplexMethod","LongMethod")
    private fun onKillEffect(entity: Entity) {
        val world = mc.level ?: return
        val pos = entity.position()
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (Effect.LIGHTNING in effects) {
            val bolt = LightningBolt(EntityType.LIGHTNING_BOLT, world)
            bolt.setPos(pos.x,pos.y,pos.z)
            bolt.setVisualOnly(true)
            world.addEntity(bolt)

            mc.soundManager.play(
                SimpleSoundInstance.forUI(
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    volume
                )
            )
        }

        if (Effect.EXPLOSION in effects) {
            mc.soundManager.play(
                SimpleSoundInstance.forUI(
                    SoundEvents.GENERIC_EXPLODE,
                    volume
                )
            )

            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.EXPLOSION,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.0, 0.0
                )
            }
        }

        if (Effect.LAVA_SPARK in effects) {
            world.levelEvent(2001, entity.blockPosition().above(1), net.minecraft.world.level.block.Block.getId(Blocks.REDSTONE_BLOCK.defaultBlockState()))

            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.LAVA,
                    x + (Math.random() - 0.5) * 1.5,
                    y + Math.random() * 1.5,
                    z + (Math.random() - 0.5) * 1.5,
                    0.0, 0.01, 0.0
                )
            }
        }

        if (Effect.GLASS_SHATTER in effects) {
            world.levelEvent(2001, entity.blockPosition().above(1), net.minecraft.world.level.block.Block.getId(Blocks.GLASS.defaultBlockState()))
            mc.soundManager.play(
                SimpleSoundInstance.forUI(
                    SoundEvents.GLASS_BREAK,
                    volume
                )
            )
        }

        if (Effect.SNOWBALL_BURST in effects) {
            repeat(particleCount) {
                val theta = Math.random() * 2 * Math.PI
                val phi = Math.random() * Math.PI

                val vx = sin(phi) * cos(theta) * 0.5
                val vy = cos(phi) * 0.5
                val vz = sin(phi) * sin(theta) * 0.5

                world.addParticle(
                    ParticleTypes.SNOWFLAKE,
                    x, y + 1.0, z,
                    vx, vy, vz
                )
            }
        }

        if (Effect.BLOOD in effects && entity is LivingEntity) {
            world.levelEvent(2001, entity.blockPosition().above(1), net.minecraft.world.level.block.Block.getId(Blocks.REDSTONE_BLOCK.defaultBlockState()))
        }

        if (Effect.FIREWORK in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.FIREWORK,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 0.1,
                    Math.random() * 0.1,
                    (Math.random() - 0.5) * 0.1
                )
            }
            mc.soundManager.play(
                SimpleSoundInstance.forUI(
                    SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    volume
                )
            )
        }

        if (Effect.TOTEM in effects && entity is LivingEntity) {
            mc.soundManager.play(
                SimpleSoundInstance.forUI(
                    SoundEvents.TOTEM_USE,
                    volume
                )
            )
            val px = x
            val py = y + entity.bbHeight * 0.5 + 0.5 // entity.height -> bbHeight
            val pz = z

            repeat(particleCount * 4) {
                val theta = Math.random() * 2.0 * Math.PI
                val phi = acos(2.0 * Math.random() - 1.0)

                val vx = sin(phi) * cos(theta)
                val vy = sin(phi) * sin(theta)
                val vz = cos(phi)

                val speed = 0.5 + Math.random() * 0.5
                world.addParticle(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    px, py, pz,
                    vx * speed,
                    vy * speed + 0.2,
                    vz * speed
                )
            }
        }

        if (Effect.SOUL in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.SOUL,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 0.05,
                    Math.random() * 0.05,
                    (Math.random() - 0.5) * 0.05
                )
            }
        }

        if (Effect.SMOKE in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.05, 0.0
                )
            }
        }

        if (Effect.FREEZE in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.SNOWFLAKE,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.0, 0.0
                )
            }
            mc.soundManager.play(
                SimpleSoundInstance.forUI(
                    SoundEvents.GLASS_BREAK,
                    volume
                )
            )
        }

        if (Effect.PORTAL in effects) {
            repeat(particleCount * 2) {
                world.addParticle(
                    ParticleTypes.REVERSE_PORTAL,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 0.1,
                    Math.random() * 0.1,
                    (Math.random() - 0.5) * 0.1
                )
            }
            mc.soundManager.play(
                SimpleSoundInstance.forUI(
                    SoundEvents.PORTAL_AMBIENT,
                    volume
                )
            )
        }

        if (Effect.SCULK in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.SCULK_SOUL,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.05, 0.0
                )
            }
        }

        if (Effect.FLASH in effects) {
            world.addParticle(
                ParticleTypes.FLASH as ParticleOptions,
                x, y + 1.0, z,
                0.0, 0.0, 0.0
            )
        }

        if (Effect.SHRIEK in effects) {
            val effect = ShriekParticleOption(0)
            val player = mc.player ?: return

            val dirX = player.x - x
            val dirY = player.eyeY - y
            val dirZ = player.z - z
            val magnitude = sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ).coerceAtLeast(0.001)
            val unitX = dirX / magnitude
            val unitY = dirY / magnitude
            val unitZ = dirZ / magnitude

            repeat(particleCount / 2) {
                val px = x + (Math.random() - 0.5) * 1.0
                val py = y + 1.0 + Math.random() * 0.5
                val pz = z + (Math.random() - 0.5) * 1.0

                world.addParticle(
                    effect,
                    px, py, pz,
                    unitX * 0.2,
                    unitY * 0.2,
                    unitZ * 0.2
                )
            }

            mc.soundManager.play(
                SimpleSoundInstance.forUI(
                    SoundEvents.WARDEN_SONIC_BOOM,
                    volume
                )
            )
        }

        if (Effect.COLORBURST in effects) {
            val from = Color4b.WHITE
            val to = Color4b.TRANSPARENT


            val effect = DustColorTransitionOptions(
                from.argb,
                to.argb,
                1.0f
            )

            repeat(particleCount) {
                world.addParticle(
                    effect,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 1.5,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.02, 0.0
                )
            }
        }
        if (Effect.WITCH in effects) {
            repeat(particleCount) {
                world.addParticle(
                    ParticleTypes.WITCH,
                    x + (Math.random() - 0.5) * 2,
                    y + Math.random() * 2,
                    z + (Math.random() - 0.5) * 2,
                    0.0, 0.0, 0.0
                )
            }
            mc.soundManager.play(
                SimpleSoundInstance.forUI(
                    SoundEvents.WITCH_AMBIENT,
                    volume
                )
            )
        }
    }
    enum class Effect(override val tag: String) : Tagged {
        BLOOD("Blood"),
        LIGHTNING("Lightning"),
        EXPLOSION("Explosion"),
        FIREWORK("Firework"),
        SOUL("Soul"),
        SMOKE("SmokeCloud"),
        FREEZE("Freeze"),
        PORTAL("Portal"),
        WITCH("WitchMagic"),
        TOTEM("Totem"),
        SCULK("Sculk"),
        FLASH("Flash"),
        SHRIEK("Shriek"),
        COLORBURST("Colorburst"),
        LAVA_SPARK("Bonfire"),
        GLASS_SHATTER("GlassShatter"),
        SNOWBALL_BURST("SnowballBurst")
    }

}


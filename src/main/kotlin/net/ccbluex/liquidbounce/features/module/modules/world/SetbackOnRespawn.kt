package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.client.gui.screen.DeathScreen
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import java.security.SecureRandom
import java.util.concurrent.ThreadLocalRandom

object SetbackOnRespawn : ClientModule("SetbackOnRespawn", Category.WORLD) {

    val modes = choices(
        "Mode", Intave1308, arrayOf(
            Intave1306,
            Intave1308,
            AAC301,
            Matrix611,
            LoyisaIntave
        )
    ).apply(::tagBy)

    private object Intave1306 : Choice("Intave1306") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        @Suppress("unused")
        private val tickHandler = handler<PlayerTickEvent> {
            if (!player.isWet && player.health < 3f) {
                repeat(30) {
                    network.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x, player.y, player.z, true,
                            false,
                        )
                    )
                }
            }
        }

        @Suppress("unused")
        private val screenHandler = handler<ScreenEvent> { event ->
            if (event.screen is DeathScreen && !player.isWet) {
                repeat(30) {
                    network.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x, player.y, player.z, true,
                            false,
                        )
                    )
                }
                player.requestRespawn()
            }
        }
    }

    private object Intave1308 : Choice("Intave1308") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        @Suppress("unused")
        private val tickHandler = handler<PlayerTickEvent> {
            if (!player.isWet && player.y >= 0 && player.health < 3f) {
                repeat(30) {
                    network.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x,
                            player.y + ThreadLocalRandom.current().nextDouble(3.0, 5.0),
                            player.z,
                            true,
                            false,
                        )
                    )
                }
            }
        }

        @Suppress("unused")
        private val screenHandler = handler<ScreenEvent> { event ->
            if (event.screen is DeathScreen && !player.isWet && player.y >= 0) {
                repeat(30) {
                    network.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x,
                            player.y + ThreadLocalRandom.current().nextDouble(3.0, 5.0),
                            player.z,
                            true,
                            false,
                        )
                    )
                }
                player.requestRespawn()
            }
        }
    }

    private val secureRandom = SecureRandom()

    private object AAC301 : Choice("AAC3") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        @Suppress("unused")
        private val tickHandler = handler<PlayerTickEvent> {
            if (player.health <= 6f) {
                player.updatePosition(
                    player.x,
                    player.y + secureRandom.nextDouble() * (5.0 - 3.0) + 3.0,
                    player.z
                )
            }
        }

        @Suppress("unused")
        private val screenHandler = handler<ScreenEvent> { event ->
            if (event.screen is DeathScreen) {
                repeat(30) { i ->
                    player.updatePosition(player.x + i, player.y + 40, player.z + i)
                }
                player.requestRespawn()
            }
        }
    }

    private object Matrix611 : Choice("Matrix6") {
        private var wasDamaged = false
        private var damageTime = 0L

        override val parent: ChoiceConfigurable<*>
            get() = modes

        @Suppress("unused")
        private val tickHandler = handler<PlayerTickEvent> {
            if (player.health <= 5f) {
                player.abilities.flying = true
                player.abilities.allowFlying = true
                player.setVelocity(0.0, 0.2, 0.0)
                wasDamaged = true
                damageTime = System.currentTimeMillis()
            } else if (wasDamaged) {
                if (System.currentTimeMillis() - damageTime < 650) {
                    player.setVelocity(0.0, 0.4, 0.0)
                } else {
                    wasDamaged = false
                    player.abilities.flying = false
                    player.abilities.allowFlying = false
                }
            }
        }

        override fun enable() {
            wasDamaged = false
            damageTime = 0L
        }
    }

    private object LoyisaIntave : Choice("Intave12") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        @Suppress("unused")
        private val screenHandler = handler<ScreenEvent> { event ->
            if (event.screen is DeathScreen) {
                repeat(18) {
                    // Send packets with position changes
                    network.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x, player.y + 4, player.z, false,
                            false
                        )
                    )
                    network.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x, player.y + 6, player.z, false,
                            false
                        )
                    )
                    network.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x, player.y - 6, player.z, false,
                            false
                        )
                    )
                    network.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x, player.y - 4, player.z, false,
                            false
                        )
                    )
                    // Final packet with onGround=true
                    network.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x, player.y, player.z, true,
                            false
                        )
                    )
                }
                player.requestRespawn()
            }
        }
    }

    override val running: Boolean
        get() = super.running
}

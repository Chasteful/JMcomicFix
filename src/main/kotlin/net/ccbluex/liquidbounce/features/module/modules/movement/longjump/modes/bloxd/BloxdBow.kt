package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.bloxd

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.item.Items

internal object BloxdBow : Choice("BloxdBow") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode

    private var isFlying = false
    private var hasTakenDamage = false
    private var shotArrow = false
    private var ticksSinceEnable = 0

    val rotations = tree(RotationsConfigurable(this))
    private val chargeTicks by int("ChargeTicks", 5, 3..20)
    private val horizontalSpeed by float("HorizontalSpeed", 2.0f, 0.1f..5f)
    private val verticalSpeed by float("VerticalSpeed", 0.5f, 0.1f..5f)
    private val maxFlyTime by int("MaxFlyTime", 2000, 500..3000,"ms")
    private var flyTimer = 0L

    private var stopMovement = false
    private var forceUseKey = false

    private fun hasBowAndArrow(): Boolean {
        val hasBow = player.inventory.main.firstOrNull { it?.item == Items.BOW } != null
        val hasArrow = player.inventory.main.firstOrNull { it?.item == Items.ARROW } != null
        return hasBow && hasArrow
    }

    val movementInputHandler = handler<MovementInputEvent> {
        if (!hasTakenDamage) {
            it.directionalInput = DirectionalInput.NONE
        } else if (isFlying) {
            // Apply custom movement during flight
            player.velocity = player.velocity.withStrafe(speed = horizontalSpeed.toDouble())

            if (mc.options.jumpKey.isPressed) {
                player.velocity = player.velocity.add(0.0, verticalSpeed.toDouble(), 0.0)
            } else if (mc.options.sneakKey.isPressed) {
                player.velocity = player.velocity.add(0.0, -verticalSpeed.toDouble(), 0.0)
            }
        }
    }
    @Suppress("unused")
    private val keyBindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.useKey && forceUseKey) {
            event.isPressed = true
        }
    }
    @Suppress("unused")
    private val tickHandler = tickHandler {
        ticksSinceEnable++

        if (!hasBowAndArrow()) {
            ModuleLongJump.disable()
            return@tickHandler
        }

        if (!hasTakenDamage) {
            // Phase 1: Prepare bow shot
            forceUseKey = true
            RotationManager.setRotationTarget(
                Rotation(player.yaw, -90f),
                configurable = rotations,
                priority = Priority.IMPORTANT_FOR_USAGE_2,
                provider = ModuleLongJump
            )

            // Stop player movement
            stopMovement = true

            // Release bow after charge time
            if (player.itemUseTime >= chargeTicks && !shotArrow) {
                network.sendPacket(PlayerMoveC2SPacket.Full(
                    player.x,
                    player.y,
                    player.z,
                    player.yaw,
                    -89.5f,
                    player.isOnGround,
                    false
                ))
                interaction.stopUsingItem(player)
                shotArrow = true
            }
        } else if (!isFlying) {
            // Phase 2: Waiting for velocity packet
            forceUseKey = false
            if (player.isUsingItem) {
                interaction.stopUsingItem(player)
            }
        } else {

            flyTimer += 50L

            if (flyTimer >= maxFlyTime) {
                ModuleLongJump.boosted = true
            }
        }
    }
    @Suppress("unused")
    private val velocityHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id && shotArrow) {
            it.cancelEvent()
            hasTakenDamage = true
            isFlying = true
            flyTimer = 0L
        }
    }

    override fun disable() {
        isFlying = false
        hasTakenDamage = false
        shotArrow = false
        ticksSinceEnable = 0
        flyTimer = 0L
        forceUseKey = false
    }
}

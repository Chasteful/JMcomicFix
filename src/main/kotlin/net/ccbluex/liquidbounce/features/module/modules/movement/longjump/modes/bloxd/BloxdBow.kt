package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.bloxd

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.notification
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
    private var originalSlot = 0

    val rotations = tree(RotationsConfigurable(this))
    private val chargeTicks by int("ChargeTicks", 5, 3..20)
    private val horizontalSpeed by float("HorizontalSpeed", 2.0f, 0.1f..5f)
    private val verticalSpeed by float("VerticalSpeed", 0.5f, 0.1f..5f)
    private val maxFlyTime by int("MaxFlyTime", 2000, 500..3000, "ms")
    private var flyTimer = 0L

    private var stopMovement = false
    private var forceUseKey = false

    private fun hasBowInHotbar(): Boolean {
        return player.inventory.main.any { it?.item == Items.BOW }
    }

    private fun hasArrowInHotbar(): Boolean {
        return player.inventory.main.any { it?.item == Items.ARROW }
    }

    private fun switchToBow(): Boolean {
        originalSlot = player.inventory.selectedSlot


        val bowSlot = player.inventory.main.indexOfFirst { it?.item == Items.BOW }
        if (bowSlot != -1) {
            player.inventory.selectedSlot = bowSlot
            return true
        }
        return false
    }

    val movementInputHandler = handler<MovementInputEvent> {
        if (!hasTakenDamage) {
            it.directionalInput = DirectionalInput.NONE
        } else if (isFlying) {

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

        if (!hasBowInHotbar() || !hasArrowInHotbar()) {
            ModuleLongJump.enabled = false
            return@tickHandler
        }
        if (!hasEnoughVerticalSpace()) {
            ModuleLongJump.enabled = false
            notification(
                "LongJump",
                "There must be a certain amount of space above you!",
                NotificationEvent.Severity.INFO
            )
            return@tickHandler
        }
        if (!hasTakenDamage) {

            if (!player.isUsingItem && player.inventory.getStack(player.inventory.selectedSlot).item != Items.BOW) {
                if (!switchToBow()) {
                    ModuleLongJump.enabled = false
                    return@tickHandler
                }
            }

            forceUseKey = true
            RotationManager.setRotationTarget(
                Rotation(player.yaw, -90f),
                configurable = rotations,
                priority = Priority.IMPORTANT_FOR_USAGE_2,
                provider = ModuleLongJump
            )

            stopMovement = true

            if (player.itemUseTime >= chargeTicks && !shotArrow) {
                network.sendPacket(
                    PlayerMoveC2SPacket.Full(
                        player.x,
                        player.y,
                        player.z,
                        player.yaw,
                        -89.5f,
                        player.isOnGround,
                        false
                    )
                )
                interaction.stopUsingItem(player)
                shotArrow = true
            }
        } else if (!isFlying) {

            forceUseKey = false
            if (player.isUsingItem) {
                interaction.stopUsingItem(player)
            }
        } else {
            flyTimer += 50L

            if (flyTimer >= maxFlyTime) {
                ModuleLongJump.boosted = true

                player.inventory.selectedSlot = originalSlot
            }
        }
    }

    private fun hasEnoughVerticalSpace(): Boolean {
        val playerPos = player.blockPos
        for (yOffset in 1..5) {
            val checkPos = playerPos.up(yOffset)
            if (!world.getBlockState(checkPos).isAir) {
                return false
            }
        }
        return true
    }

    @Suppress("unused")
    private val velocityHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id && shotArrow) {
            it.cancelEvent()
            hasTakenDamage = true
            isFlying = true
            flyTimer = 0L
            player.inventory.selectedSlot = originalSlot
        }
    }

    override fun disable() {
        isFlying = false
        hasTakenDamage = false
        shotArrow = false
        ticksSinceEnable = 0
        flyTimer = 0L
        forceUseKey = false
        player.inventory.selectedSlot = originalSlot
    }
}


package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.isInteractable
import net.ccbluex.liquidbounce.utils.client.PacketSnapshot
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.Items
import net.minecraft.item.consume.UseAction
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand

internal object VelocityGrimFull : VelocityMode("GrimACFull") {
    private val maxStuckTicks by int("MaxStuckTicks", 5, 1..100, "ticks")
    private val onlyOnGround by boolean("OnlyOnGround", true)

    private object PlaceWater : ToggleableConfigurable(this, "PlaceWater", false) {
        val rotationsConfigurable = tree(RotationsConfigurable(this))
    }

    init {
        tree(PlaceWater)
    }

    private var canCancel = false
    private var delay = false
    private var needClick = false
    private var waitForUpdate = false
    private var shouldSkip = false
    private var needPlaceWaterRotation = false
    private var needPlaceWater = false
    private val delayedPacketQueue = Queues.newConcurrentLinkedQueue<PacketSnapshot>()

    override fun enable() {
        canCancel = false
        delay = false
        needClick = false
        waitForUpdate = false
        shouldSkip = false
        needPlaceWaterRotation = false
        needPlaceWater = false
        delayedPacketQueue.clear()
    }

    override fun disable() {
        delayedPacketQueue.forEach { handlePacket(it.packet) }
        delayedPacketQueue.clear()
    }

    @Suppress("unused", "DEPRECATION","ComplexCondition")
    private val packetEventHandler = sequenceHandler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerInteractEntityC2SPacket || packet is PlayerInteractBlockC2SPacket) {
            shouldSkip = true
        }

        if (packet is PlayerMoveC2SPacket && packet.changePosition && waitForUpdate) {
            event.cancelEvent()
        }

        if (event.isCancelled || event.origin == TransferOrigin.OUTGOING) {
            return@sequenceHandler
        }

        if (waitForUpdate && packet is BlockUpdateS2CPacket && packet.pos.equals(player.blockPos)) {
            waitTicks(1)
            waitForUpdate = false
            needClick = false
            if (PlaceWater.enabled) needPlaceWaterRotation = true
            return@sequenceHandler
        }

        if (waitForUpdate) {
            return@sequenceHandler
        }

        if (delay) {
            delayedPacketQueue.add(PacketSnapshot(packet, event.origin, System.currentTimeMillis()))
            event.cancelEvent()
            return@sequenceHandler
        }

        if (packet is EntityDamageS2CPacket && packet.entityId == player.id) {
            canCancel = true
        }

        if (((packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id)
                || packet is ExplosionS2CPacket)
            && canCancel
        ) {
            val hitResult = raycast(rotation = Rotation(player.yaw, 90f))
            val pos = hitResult.blockPos.offset(hitResult.side)
            val blockState = world.getBlockState(hitResult.blockPos)
            if (player.activeItem.useAction != UseAction.EAT
                && player.activeItem.useAction != UseAction.DRINK
                && !InventoryManager.isInventoryOpen
                && mc.currentScreen !is GenericContainerScreen
                && (!onlyOnGround || player.isOnGround)
                && !hitResult.blockPos.getBlock().isInteractable(blockState)
                && blockState.isSolid
                && blockState.isOpaqueFullCube
                && (!PlaceWater.enabled || Slots.OffhandWithHotbar.findClosestSlot(Items.WATER_BUCKET) != null)
            ) {
                event.cancelEvent()
                if (PlaceWater.enabled) needPlaceWaterRotation = true
                delay = true
                needClick = true
            }
            canCancel = false
        }
    }

    @Suppress("unused")
    private val playerTickEventHandler = handler<PlayerTickEvent> { event ->
        if (needClick) {
            val pitch = 90f - (0.01f..0.1f).random()
            val hitResult = raycast(rotation = Rotation(player.yaw, pitch))
            val pos = hitResult.blockPos.offset(hitResult.side)

            if (pos.equals(player.blockPos) && !shouldSkip) {
                delay = false
                delayedPacketQueue.forEach { handlePacket(it.packet) }
                delayedPacketQueue.clear()

                if (interaction.interactBlock(player, Hand.MAIN_HAND, hitResult) == ActionResult.SUCCESS) {
                    player.swingHand(Hand.MAIN_HAND)
                }

                if (RotationManager.serverRotation.pitch != pitch) {
                    network.sendPacket(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            player.yaw,
                            pitch,
                            player.isOnGround,
                            player.horizontalCollision
                        )
                    )
                } else {
                    network.sendPacket(
                        PlayerMoveC2SPacket.OnGroundOnly(
                            player.isOnGround,
                            player.horizontalCollision
                        )
                    )
                }

                waitForUpdate = true
                needClick = false
            }
        }

        if (waitForUpdate) {
            event.cancelEvent()
        }

        shouldSkip = false
    }

    @Suppress("unused")
    private val rotationUpdateEventHandler = handler<RotationUpdateEvent> {
        if (!PlaceWater.enabled || !needPlaceWaterRotation) return@handler

        RotationManager.setRotationTarget(
            rotation = Rotation(
                player.yaw - (0.002f..0.004f).random(),
                90f - (0.002f..0.004f).random()
            ),
            configurable = PlaceWater.rotationsConfigurable,
            priority = Priority.IMPORTANT_FOR_USER_SAFETY,
            provider = ModuleVelocity,
        )

        needPlaceWaterRotation = false
        needPlaceWater = true
    }

    @Suppress("unused")
    private val placeWaterHandler = tickHandler {
        if (!PlaceWater.enabled) return@tickHandler

        waitUntil { needPlaceWater }

        val waterBucket = Slots.OffhandWithHotbar.findClosestSlot(Items.WATER_BUCKET)!!
        SilentHotbar.selectSlotSilently(this, waterBucket, 1)

        doPlacement(
            rayTraceResult = raycast(),
            hand = waterBucket.useHand
        )

        needPlaceWater = false
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        waitUntil { waitForUpdate }

        repeat(maxStuckTicks) {
            waitTicks(1)
            if (!waitForUpdate) return@tickHandler
        }

        waitForUpdate = false
        needClick = false
    }



}

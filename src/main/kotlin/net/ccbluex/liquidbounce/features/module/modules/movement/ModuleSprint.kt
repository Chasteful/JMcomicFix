package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSprintControlFeature
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.util.math.MathHelper

/**
 * Sprint module
 *
 * Sprints automatically.
 */

object ModuleSprint : ClientModule("Sprint", Category.MOVEMENT) {

    private enum class SprintMode(override val choiceName: String) : NamedChoice {
        LEGIT("Legit"),
        OMNIDIRECTIONAL("Omnidirectional"),
        OMNIROTATIONAL("Omnirotational"),
    }

    private val sprintMode by enumChoice("Mode", SprintMode.LEGIT)

    private val ignore by multiEnumChoice<Ignore>("Ignore")

    /**
     * This is used to stop sprinting when the player is not moving forward
     * without a velocity fix enabled.
     */
    private val stopOn by multiEnumChoice("StopOn", StopOn.entries)

    val shouldSprintOmnidirectional: Boolean
        get() = running && sprintMode == SprintMode.OMNIDIRECTIONAL ||
            ScaffoldSprintControlFeature.allowOmnidirectionalSprint

    val shouldIgnoreBlindness
        get() = running && Ignore.BLINDNESS in ignore

    val shouldIgnoreHunger
        get() = running && Ignore.HUNGER in ignore

    val shouldIgnoreCollision
        get() = running && Ignore.COLLISION in ignore

    val shouldIgnoreLiquid
        //Swimming and sprinting cannot be done at the same time
        get() = running && Ignore.LIQUID in ignore && !player.isSwimming


    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (!event.directionalInput.isMoving) {
            return@handler
        }

        if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
            event.sprint = true
        }
    }

    @Suppress("unused")
    private val sprintPreventionHandler = handler<SprintEvent> { event ->
        // In this case we want to prevent sprinting on movement tick only,
        // because otherwise you could guess from the input change that this is automated.
        if (event.source == SprintEvent.Source.MOVEMENT_TICK && shouldPreventSprint()) {
            event.sprint = false
        }
    }
    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> { event ->
        if (sprintMode == SprintMode.OMNIDIRECTIONAL && shouldSprintOmnidirectional) {
            // Allows us to sprint boost in every direction
            event.yaw = getMovementDirectionOfInput(player.yaw, DirectionalInput(player.input))
        }
    }
    // DO NOT USE TREE TO MAKE SURE THAT THE ROTATIONS ARE NOT CHANGED
    private val rotationsConfigurable = RotationsConfigurable(this)

    @Suppress("unused")
    private val omniRotationalHandler = handler<GameTickEvent> {
        // Check if omnirotational sprint is enabled
        if (sprintMode != SprintMode.OMNIROTATIONAL) {
            return@handler
        }

        val yaw = getMovementDirectionOfInput(player.yaw, DirectionalInput(player.input))

        // todo: unhook pitch - AimPlan needs support for only yaw or pitch operation
        val rotation = Rotation(yaw, player.pitch)

        RotationManager.setRotationTarget(
            rotationsConfigurable.toRotationTarget(rotation), Priority.NOT_IMPORTANT,
            this@ModuleSprint
        )
    }

    private fun getChestScreen(): GenericContainerScreen? {
        val screen = mc.currentScreen

        return screen as? GenericContainerScreen
    }

    @Suppress("MagicNumber")
    fun shouldPreventSprint(): Boolean {
        // Check if chest screen is open and StopOn.CHEST is enabled
        if (StopOn.CHEST in stopOn && getChestScreen() != null) {
            return true
        }

        val deltaYaw = player.yaw - (RotationManager.currentRotation ?: return false).yaw
        val (forward, sideways) = Pair(player.input.movementForward, player.input.movementSideways)

        val hasForwardMovement = forward * MathHelper.cos(deltaYaw * 0.017453292f) + sideways *
            MathHelper.sin(deltaYaw * 0.017453292f) > 1.0E-5
        val preventSprint = (if (player.isOnGround) StopOn.GROUND in stopOn else StopOn.AIR in stopOn)
            && !shouldSprintOmnidirectional
            && RotationManager.activeRotationTarget?.movementCorrection == MovementCorrection.OFF
            && !hasForwardMovement

        return running && preventSprint
    }

    private enum class Ignore(override val choiceName: String) : NamedChoice {
        BLINDNESS("Blindness"),
        HUNGER("Hunger"),
        COLLISION("Collision"),
        LIQUID("Liquid"),
    }

    private enum class StopOn(override val choiceName: String) : NamedChoice {
        GROUND("Ground"),
        AIR("Air"),
        CHEST("Chest")
    }
}

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.jmcomicfix.features.module.modules.world.stuck.ModuleAutoStuck
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAirJump
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

@Suppress("TooManyFunctions")
object ModuleAutoClutch : ClientModule("AutoClutch", Category.PLAYER) {
    init {
        enableLock()
    }

    @Suppress("unused")
    private val mode by enumChoice("Algorithm", Algorithm.SimulatedAnnealing)

    private val maxIterations by int("MaxIterations", 25000, 2000..50000)
    private val maxRepeat by int("MaxRepeat", 50000, 2000..50000)
    private val initialTemperature by float("InitialTemp", 15f, 5f..30f)
    private val minTemperature by float("MinTemp", 0.01f, 0.01f..0.1f)
    private val coolingRate by float("CoolingRate", 0.99f, 0.95f..0.99f)
    private val aimThreshold by float("AimThreshold", 0.5f, 0.1f..1f)
    private val aimPitch by floatRange("PitchRange", -90f..90f, -90f..90f)
    private val slotResetDelay by int("SlotResetDelay", 1, 0..10, "ticks")
    private var cooldownTicks by int("Cooldown", 1, 1..20, "ticks")
    private var safetyCheckTicks by int("SafetyCheck", 10, 5..20, "ticks")
    private val voidThreshold by int("VoidLevel", 0, -256..0)
    private val moduleChecks by multiEnumChoice("ModuleChecks",
        ModuleCheck.WHILE_SCAFFOLD,
        ModuleCheck.WHILE_LONG_JUMP,
        ModuleCheck.WHILE_FLY,
        ModuleCheck.WHILE_AIR_JUMP,
        ModuleCheck.WHILE_ELYTRA_FLY)

    private val silenceSlot by boolean("SilenceHotbar", true)
    private val needStuck by boolean("NeedStuck", false)
    private val rotationConfig = tree(RotationsConfigurable(this))

    enum class State { IDLE, FINDING_PEARL, CALCULATING, ROTATING, THROWING }

    var state = State.IDLE
    private const val SAFE_TICKS_THRESHOLD = 10
    private const val PEARL_LANDING_TIME = 30

    private var bestEnergy = Double.MAX_VALUE
    private var currentSolution = Rotation(0f, 0f)
    private var currentEnergy = Double.MAX_VALUE
    private var temperature = initialTemperature
    private var iterations = 0
    private var lastPearlThrowTime = 0L
    private var safetyCheckCounter = 0
    private var triggerPosition: Vec3d? = null
    private var pearlSlot: HotbarItemSlot? = null
    private var lastPlayerPosition: Vec3d? = null
    private var bestSolution: Rotation? = null
    private var isLikelyFallingIntoVoid = false
    private var safetyCheckActive = false

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (needStuck && !ModuleAutoStuck.shouldEnableStuck) {
            return@handler
        }
        if( mc.options.sneakKey.isPressed) {
            return@handler
        }
        if (safetyCheckActive) {
            safetyCheckCounter--
            if (safetyCheckCounter <= 0) {
                safetyCheckActive = false
                if (isPlayerSafe()) {
                    state = State.IDLE
                } else {
                    triggerPosition = player.pos
                    state = State.FINDING_PEARL
                }
            }
        }

        checkPlayerMovement()
        checkVoidFall()

        when (state) {
            State.IDLE -> checkActivationConditions()
            State.FINDING_PEARL -> findPearl()
            State.CALCULATING -> calculateSolution()
            State.ROTATING -> rotateToSolution()
            State.THROWING -> throwPearl()
        }
    }
    private fun canReachSafeBlock(): Boolean {
        val world = mc.world ?: return false
        val player = mc.player ?: return false


        var motionX = player.velocity.x
        var motionY = player.velocity.y
        var motionZ = player.velocity.z

        var posX = player.pos.x
        var posY = player.pos.y
        var posZ = player.pos.z

        for (tick in 0 until 20) {

            motionY -= 0.08
            motionY *= 0.98

            motionX *= 0.91
            motionZ *= 0.91

            posX += motionX
            posY += motionY
            posZ += motionZ

            val playerBox = player.boundingBox.offset(posX - player.pos.x, posY - player.pos.y, posZ - player.pos.z)
            val collisions = world.getBlockCollisions(player, playerBox)

            if (collisions.iterator().hasNext()) {
                val blockPos = BlockPos(posX.toInt(), (posY - 0.5).toInt(), posZ.toInt())
                val blockState = world.getBlockState(blockPos)
                return !blockState.isAir && blockState.block != Blocks.WATER && blockState.block != Blocks.LAVA
            }

            if (posY <= voidThreshold.toDouble()) {
                return false
            }
        }

        return false
    }
    private fun checkVoidFall() {
        isLikelyFallingIntoVoid = isPredictingFall() && !canReachSafeBlock()
    }

    private fun isPredictingFall(): Boolean {
        val world = mc.world ?: return false
        val player = mc.player ?: return false

        var motionX = player.velocity.x
        var motionY = player.velocity.y
        var motionZ = player.velocity.z

        var posX = player.pos.x
        var posY = player.pos.y
        var posZ = player.pos.z

        for (tick in 0 until SAFE_TICKS_THRESHOLD) {
            motionY -= 0.08
            motionY *= 0.98
            motionX *= 0.91
            motionZ *= 0.91

            posX += motionX
            posY += motionY
            posZ += motionZ

            val playerBox = player.boundingBox.offset(posX - player.pos.x, posY - player.pos.y, posZ - player.pos.z)
            val collisions = world.getBlockCollisions(player, playerBox)

            if (collisions.iterator().hasNext()) {
                return false
            }

            if (posY <= voidThreshold.toDouble()) {
                return true
            }
        }

        return posY <= voidThreshold.toDouble()
    }

    private fun isInVoid(pos: Vec3d): Boolean {
        val boundingBox = player.boundingBox
            .offset(pos.subtract(player.pos))
            .withMinY(voidThreshold.toDouble())

        val collisions = world.getBlockCollisions(player, boundingBox)
        return collisions.none() || collisions.all { shape -> shape == VoxelShapes.empty() }
    }

    private fun checkPlayerMovement() {
        val currentPos = player.pos
        lastPlayerPosition?.let { lastPos ->
            if (currentPos.distanceTo(lastPos) > 1.0) {
                resetAllVariables()
            }
        }
        lastPlayerPosition = currentPos
    }

    private fun resetAllVariables() {
        state = State.IDLE
        bestSolution = null
        bestEnergy = Double.MAX_VALUE
        currentSolution = Rotation(0f, 0f)
        currentEnergy = Double.MAX_VALUE
        temperature = initialTemperature
        iterations = 0
        triggerPosition = null
        pearlSlot = null
        isLikelyFallingIntoVoid = false
        safetyCheckCounter = 0
        safetyCheckActive = false
    }

    private fun checkActivationConditions() {
        if (System.currentTimeMillis() - lastPearlThrowTime < 1000L) {
            return
        }

        // 检查所有选中的模块是否允许运行
        if (moduleChecks.any { !it.testCondition() }) {
            return
        }

        if (isLikelyFallingIntoVoid || (!ModuleAutoStuck.isInAir && !isBlockUnder(
                x = player.pos.x,
                y = player.pos.y,
                z = player.pos.z,
                height = 30.0,
                boundingBox = true
            ) && !canReachSafeBlock())
        ) {
            triggerPosition = player.pos
            state = State.FINDING_PEARL
        }
    }

    private fun findPearl() {
        val pearlSlot = Slots.OffhandWithHotbar.findSlot(Items.ENDER_PEARL)?.hotbarSlotForServer
        if (pearlSlot == null){
            state = State.IDLE
            return
        }

        if (silenceSlot) {
            SilentHotbar.selectSlotSilently(this, pearlSlot, slotResetDelay)
        } else {
            player.inventory.selectedSlot = pearlSlot
        }

        resetAnnealing()
        state = State.CALCULATING
    }

    private fun resetAnnealing() {
        currentSolution = Rotation(
            getRandomInRange(-180f, 180f).toFloat(),
            getRandomInRange(-90f, 90f).toFloat(),
        )

        currentEnergy = assessRotation(currentSolution)
        bestSolution = currentSolution
        bestEnergy = currentEnergy

        temperature = initialTemperature
        iterations = 0
    }

    fun getRandomInRange(min: Float, max: Float): Int {
        return (Math.random() * (max - min).toDouble() + min.toDouble()).toInt()
    }

    private fun calculateSolution() {
        val scaledTemperature = temperature * 100
        repeat(maxRepeat) {
            if (iterations >= maxIterations || temperature < minTemperature) {
                state = State.ROTATING
                return@repeat
            }

            val newSolution = Rotation(
                (currentSolution.yaw + getRandomInRange(-temperature * 18f, temperature * 18f)),
                (currentSolution.pitch + getRandomInRange(-temperature * 9f, temperature * 9f))
                    .coerceIn(aimPitch)
            )

            val newEnergy = assessRotation(newSolution)
            val deltaEnergy = newEnergy - currentEnergy

            if (deltaEnergy < 0 || Random.Default.nextDouble() < exp(-deltaEnergy / temperature)) {
                currentSolution = newSolution
                currentEnergy = newEnergy

                if (currentEnergy < bestEnergy) {
                    bestSolution = currentSolution
                    bestEnergy = currentEnergy
                }
            }

            temperature = (scaledTemperature * coolingRate) / 100
            iterations++
        }
    }

    private fun rotateToSolution() {
        if (!mc.options.pickItemKey.isPressed) {
            bestSolution?.let {
                RotationManager.setRotationTarget(
                    rotationConfig.toRotationTarget(it),
                    priority = Priority.IMPORTANT_FOR_USAGE_1,
                    provider = this,
                )

                if (RotationManager.serverRotation.angleTo(it) <= aimThreshold) {
                    state = State.THROWING
                }
            } ?: run {
                state = State.IDLE
            }
        }
    }

    private fun throwPearl() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastPearlThrowTime < cooldownTicks * 50L) {
            state = State.IDLE
            return
        }

        if (!isLikelyFallingIntoVoid && isPlayerSafe()) {
            state = State.IDLE
            return
        }

        Slots.Hotbar.findSlot(Items.ENDER_PEARL)?.let {
            useHotbarSlotOrOffhand(it, 0, bestSolution?.yaw ?: 0f, bestSolution?.pitch ?: 0f)
            lastPearlThrowTime = currentTime

            scheduleSafetyCheck()
        }

        state = State.IDLE
    }

    private fun isPlayerSafe(): Boolean {
        if (!isInVoid(player.pos)) {
            return true
        }

        for (tick in 0 until safetyCheckTicks) {
            val futurePos = player.pos.add(0.0, -tick.toDouble(), 0.0)
            if (!isInVoid(futurePos)) {
                return true
            }
        }

        return false
    }

    private fun scheduleSafetyCheck() {
        safetyCheckCounter = PEARL_LANDING_TIME
        safetyCheckActive = true
    }

    private fun assessRotation(rotation: Rotation): Double {
        val pearlPos = simulatePearlTrajectory(rotation)
        return assessPosition(pearlPos)
    }

    private fun assessPosition(pos: Vec3d): Double {
        val groundPos = BlockPos(pos.x.toInt(), (pos.y - 0.5).toInt(), pos.z.toInt())
        val groundState = world.getBlockState(groundPos)
        val hasGround = !groundState.isAir && groundState.block != Blocks.WATER && groundState.block != Blocks.LAVA

        val playerBox = Box(
            pos.x - 0.3, pos.y, pos.z - 0.3,
            pos.x + 0.3, pos.y + player.height, pos.z + 0.3
        )
        val collisions = world.getBlockCollisions(player, playerBox).iterator()
        val hasSpace = !collisions.hasNext()

        val horizontalDistance = distanceSq2D(pos, triggerPosition ?: player.pos)

        return when {
            !hasGround -> 10000.0
            !hasSpace -> 5000.0 + horizontalDistance
            else -> horizontalDistance
        }
    }

    private fun simulatePearlTrajectory(rotation: Rotation): Vec3d {
        val yaw = Math.toRadians(rotation.yaw.toDouble())
        val pitch = Math.toRadians(rotation.pitch.toDouble())

        val velocity = 1.5

        var motionX = -sin(yaw) * cos(pitch) * velocity
        var motionY = -sin(pitch) * velocity
        var motionZ = cos(yaw) * cos(pitch) * velocity

        var posX = player.x
        var posY = player.eyeY
        var posZ = player.z

        repeat(40) {
            motionY -= 0.03
            posX += motionX
            posY += motionY
            posZ += motionZ
            motionX *= 0.99
            motionY *= 0.99
            motionZ *= 0.99
        }

        return Vec3d(posX, posY, posZ)
    }

    private fun distanceSq2D(a: Vec3d, b: Vec3d): Double {
        val dx = a.x - b.x
        val dz = a.z - b.z
        return dx * dx + dz * dz
    }

    private fun isBlockUnder(x: Double, y: Double, z: Double, height: Double, boundingBox: Boolean): Boolean {
        val world = mc.world ?: return false
        val player = mc.player ?: return false

        if (boundingBox) {
            var offset = 0.0
            while (offset < height) {
                val motionX = player.velocity.x
                val motionZ = player.velocity.z
                val playerBox = player.boundingBox.offset(motionX * offset, -offset, motionZ * offset)

                if (world.getBlockCollisions(player, playerBox).iterator().hasNext()) {
                    return true
                }
                offset += 0.5
            }
        } else {
            var offset = 0.0
            while (offset < height) {
                val blockPos = BlockPos(x.toInt(), (y - offset).toInt(), z.toInt())
                val blockState = world.getBlockState(blockPos)

                if (!blockState.isAir && blockState.fluidState.isEmpty) {
                    return true
                }
                offset += 0.5
            }
        }
        return false
    }

    enum class Algorithm(override val choiceName: String) : NamedChoice {
        SimulatedAnnealing("SimulatedAnnealing")
    }

    @Suppress("unused")
    private enum class ModuleCheck(
        override val choiceName: String,
        val testCondition: () -> Boolean
    ) : NamedChoice {
        WHILE_SCAFFOLD("WhileScaffold", { !ModuleScaffold.running }),
        WHILE_LONG_JUMP("WhileLongJump", { !ModuleLongJump.running }),
        WHILE_FLY("WhileFly", { !ModuleFly.running }),
        WHILE_ELYTRA_FLY("WhileElytraFly", { !ModuleElytraFly.running }),
        WHILE_AIR_JUMP("WhileAirJump", { !ModuleAirJump.running }),
        WHILE_FREEZE("WhileFreeze", { !ModuleFreeze.running })
    }

    override fun disable() {
        resetAllVariables()
        lastPlayerPosition = null
    }
}

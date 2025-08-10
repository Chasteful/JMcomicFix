@file:Suppress("detekt:all")
package net.ccbluex.liquidbounce.features.module.modules.misc.anticheat

import net.ccbluex.jmcomicfix.utils.technology.CheaterDetectorUtil
import net.ccbluex.jmcomicfix.utils.technology.CheaterDetectorUtil.isNotValidToCheck
import net.ccbluex.jmcomicfix.utils.technology.ComplexMath
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.warning
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.util.math.MathHelper
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs


object ModuleAntiCheat : ClientModule("AuroraAntiCheat", Category.MISC) {

    private object CheckSettings : ToggleableConfigurable(this, "AllowCheck", true) {
        val celerity by boolean("Celerity", true)
        val noSlowDown by boolean("NoSlowDown", true)
        val flight by boolean("Flight", true)
        val sprint by boolean("Sprint", true)
        val jump by boolean("Jump", true)
        val autoBlock by boolean("AutoBlock", true)
        val aura by boolean("Aura", true)
        val aimAssist by boolean("AimAssist", true)
        val velocity by boolean("Velocity", true)
        val interactDistance by boolean("InteractDistance", true)
        val spoofGround by boolean("SpoofGround", true)
        val prediction by boolean("Prediction", true)
        val exploit by boolean("Exploit", true)
        val autoClick by boolean("AutoClick", true)
    }

    val tolerant by float("tolerant",0.05f,0.05f..0.1f)
    private val maxAimSpeed by float("MaxAimSpeed", 120f, 1f..180f, "°/s")
    private val maxClickSpeed by int("MaxClickSpeed", 20, 1..100, "cps")
    private val minTickBalance by int("MinTickBalance", 45, 5..100, "ticks")
    private val sprintRange by float("SprintRange", 3.7f, 1f..7f, "blocks")
    private val normalRange by float("NormalRange", 3.3f, 1f..7f, "blocks")
    private val airSpeedFactor by float("AirSpeedFactor", 1.25f, 0f..8f, "x")
    private val groundSpeedFactor by float("GroundSpeedFactor", 1.15f, 0f..8f, "x")
    private val jumpExtraSpeed by float("JumpExtraSpeed", 0.2f, 0f..8f, "blocks/tick")
    private val webFactor by float("WebSpeedFactor", 0.5f, 0f..8f, "x")
    private val slowDownFactor by float("SlowSpeedFactor", 0.5f, 0f..8f, "x")

    private val checkSelf by boolean("CheckSelf", true)
    private val checkOtherPlayers by boolean("CheckOtherPlayers", false)
    private val checkServerBot by boolean("CheckServerBot", false)

    init {
        tree(CheckSettings)
    }

    private val hackData = ConcurrentHashMap<Int, HackerLivingBase>()

    override fun enable() {
        chat(warning(translation("liquidbounce.module.disabler.messages.auroraAntiCheat")))
    }

    override fun disable() {
        hackData.clear()
    }
    @Suppress("UNUSED")
    private val packetHandler = handler<PacketEvent> { event ->
        if (mc.world == null || mc.player == null) return@handler

        if (mc.player!!.age < 5) hackData.clear()

        when (val packet = event.packet) {
            is EntityStatusS2CPacket -> {
                if (packet.status == 2.toByte()) {
                    checkCombatHurt(packet.getEntity(mc.world))
                }
            }
            is EntityTrackerUpdateS2CPacket -> {
                val entity = mc.world?.getEntityById(packet.id) ?: return@handler
                if (entity !is PlayerEntity) return@handler

                hackData.computeIfAbsent(entity.id) { HackerLivingBase(entity) }
                val hacker = hackData[entity.id] ?: return@handler
                hacker.onPre(entity)
                checkMovement(hacker)
                hacker.onPost()
            }
            is EntityVelocityUpdateS2CPacket -> {
                hackData[packet.entityId]?.let { hacker ->
                    hacker.bypassTime = System.currentTimeMillis()
                    hacker.velocityVector = Vec3(
                        packet.velocityX.toDouble(),
                        packet.velocityY.toDouble(),
                        packet.velocityZ.toDouble()
                    )
                }
            }
            is ExplosionS2CPacket -> {
                hackData.values.forEach { hacker ->
                    val velocityOpt = packet.playerKnockback
                    if (velocityOpt.isPresent) {
                        val velocity = velocityOpt.get()
                        if (hacker.player.squaredDistanceTo(velocity.x, velocity.y, velocity.z) < 8.0) {
                            hacker.velocityVector = Vec3(31200.0, 31200.0, 31200.0)
                            hacker.bypassTime = System.currentTimeMillis()
                        }
                    }
                }
            }

            is PlayerInteractEntityC2SPacket -> {
                hackData[mc.player?.id]?.let { hacker ->
                    if (System.currentTimeMillis() - hacker.lastCT > 5000) hacker.samples.clear()
                    hacker.samples.add((System.currentTimeMillis() - hacker.lastCT).toInt())
                    hacker.lastCT = System.currentTimeMillis()
                    if (hacker.samples.size >= 40) {
                        checkAutoClick(hacker)
                        hacker.samples.clear()
                    }
                }
            }
        }
    }
    @Suppress("UNUSED")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        hackData.clear()
    }
    @Suppress("UNUSED")
    private val motionHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        if (event.state == EventState.POST) {
            val playerId = mc.player?.id ?: return@handler
            hackData.computeIfAbsent(playerId) { HackerLivingBase(mc.player!!) }
            hackData[playerId]?.let { hacker ->
                checkMovement(hacker)
                hacker.onPost()
            }
        }
    }

    fun getFovToTarget(attacker: Entity, target: Entity): Float {
        val x = target.x - attacker.x
        val y = target.y + target.eyeY - 0.15 - attacker.y
        val z = target.z - attacker.z
        val calcYaw = (MathHelper.atan2(z, x) * 180.0 / Math.PI - 90.0).toFloat()
        val calcPitch = (-MathHelper.atan2(y,
            MathHelper.sqrt((x * x + z * z).toFloat()).toDouble()) * 180.0 / Math.PI).toFloat()
        val diffYaw = MathHelper.wrapDegrees(calcYaw - attacker.yaw)
        val diffPitch = MathHelper.wrapDegrees(calcPitch - attacker.pitch)
        return ComplexMath.hypot(diffYaw.toDouble(), diffPitch.toDouble()).toFloat()
    }

    private fun checkCombatHurt(entity: Entity?) {
        if (entity == null) return
        val attacker = mc.world?.entities?.filterIsInstance<PlayerEntity>()
            ?.filter { it != entity && it.distanceTo(entity) <= 10.0 }
            ?.minByOrNull { it.distanceTo(entity) + getFovToTarget(it, entity) / 40.0 }
            ?: return

        hackData.computeIfAbsent(attacker.id) { HackerLivingBase(attacker) }
        val hacker = hackData[attacker.id] ?: return
        if (isNotValidToCheck(hacker.player, checkSelf, !checkServerBot, checkOtherPlayers)) return

        if (CheckSettings.aura) {
            val result = CheaterDetectorUtil.checkKillAura(hacker, entity as LivingEntity)
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "Aura")
        }
        if (CheckSettings.aimAssist) {
            val result = CheaterDetectorUtil.checkAimAssist(hacker, entity as LivingEntity, maxAimSpeed.toDouble())
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "AimAssist")
            hacker.updateDRot()
            val fixedYaw = (hacker.player.yaw % 360 + 540) % 360 - 180
            val fixedPrevYaw = (hacker.player.prevYaw % 360 + 540) % 360 - 180
            val deltaYaw = abs(fixedPrevYaw - fixedYaw)
            val deltaPitch = abs(hacker.player.pitch - hacker.player.prevPitch)
            hacker.updateAccel(deltaYaw, deltaPitch)
        }
        if (CheckSettings.autoBlock) {
            val result = CheaterDetectorUtil.checkAutoBlock(hacker)
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "AutoBlock")
        }
        if (CheckSettings.interactDistance) {
            val result = CheaterDetectorUtil.checkReach(hacker, entity as LivingEntity, sprintRange.toDouble(), normalRange.toDouble())
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "InteractDistance")
        }
    }

    private fun checkMovement(hacker: HackerLivingBase) {
        if (isNotValidToCheck(hacker.player, checkSelf, !checkServerBot, checkOtherPlayers)) return
        hacker.balance.add((System.currentTimeMillis() - hacker.balanceTime).toInt())
        hacker.balanceTime = System.currentTimeMillis()
        if (hacker.player.age % 160 == 0 && hacker.vl > 0) hacker.vl--

        if (hacker.player.fallDistance <= 0.001) hacker.jumpTick = 0 else hacker.jumpTick++

        if (CheckSettings.exploit) {
            if (hacker.player.isCreative && !hacker.player.abilities.allowFlying) {
                CheaterDetectorUtil.flagEntity(hacker, "Flying but not allowed.", "Exploit")
            }
            if (hacker.balance.size >= 20) {
                val avg = ComplexMath.getAverage(hacker.balance)
                hacker.balance.clear()
                if (avg < minTickBalance) {
                    CheaterDetectorUtil.flagEntity(hacker, "Sending packets too quickly (AVG=$avg)", "Exploit")
                }
            }
        }
        if (CheckSettings.velocity && System.currentTimeMillis() - hacker.bypassTime > 70) {
            val result = CheaterDetectorUtil.checkVelocity(hacker)
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "Velocity")
        }
        if (CheckSettings.sprint) {
            val result = CheaterDetectorUtil.checkExperimentalSprint(hacker)
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "Sprint")
        }
        if (CheckSettings.prediction && System.currentTimeMillis() - hacker.bypassTime > 70) {
            val result = CheaterDetectorUtil.checkPrediction(hacker)
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "Prediction")
        }
        if (CheckSettings.flight && System.currentTimeMillis() - hacker.bypassTime > 500) {
            val result = CheaterDetectorUtil.checkPhysic(hacker)
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "Flight")
        }
        if (CheckSettings.noSlowDown && System.currentTimeMillis() - hacker.bypassTime > 170) {
            val result = CheaterDetectorUtil.checkSlowDown(hacker, slowDownFactor.toDouble(), airSpeedFactor.toDouble(), jumpExtraSpeed.toDouble(), groundSpeedFactor.toDouble(), webFactor.toDouble())
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "NoSlowDown")
        }
        if (CheckSettings.celerity && System.currentTimeMillis() - hacker.bypassTime > 170) {
            val result = CheaterDetectorUtil.checkCelerity(hacker, airSpeedFactor.toDouble(), jumpExtraSpeed.toDouble(), groundSpeedFactor.toDouble())
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "Celerity")
        }
        if (CheckSettings.jump && System.currentTimeMillis() - hacker.bypassTime > 170) {
            val result = CheaterDetectorUtil.checkJump(hacker)
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "Jump")
        }
        if (CheckSettings.spoofGround) {
            val result = CheaterDetectorUtil.checkGround(hacker)
            if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "SpoofGround")
        }
    }

    private fun checkAutoClick(hacker: HackerLivingBase) {
        if (!CheckSettings.autoClick || isNotValidToCheck(hacker.player, checkSelf, !checkServerBot, checkOtherPlayers)) return
        val result = CheaterDetectorUtil.checkAutoClick(hacker.samples, maxClickSpeed)
        if (result != "LEGIT") CheaterDetectorUtil.flagEntity(hacker, result, "AutoClick")
    }

    class HackerLivingBase(var player: PlayerEntity) {
        var lastTickPlayer: PlayerEntity = player
        var bypassTime: Long = System.currentTimeMillis()
        var balanceTime: Long = System.currentTimeMillis()
        var velocityVector: Vec3 = Vec3(0.0, 0.0, 0.0)
        var lastMotionY: Double = player.pos.y - player.prevPos.y
        var samples: ArrayList<Int> = ArrayList()
        var balance: ArrayList<Int> = ArrayList()
        var vl: Int = 0
        var lastCT: Long = 0
        var jumpTick: Int = 0
        var lastDYaw: Float = 0f
        var lastDPitch: Float = 0f
        var lastYAccel: Float = 0f
        var lastPAccel: Float = 0f

        fun onPre(sp: PlayerEntity) {
            player = sp
        }

        fun onPost() {
            lastMotionY = player.pos.y - player.prevPos.y
            lastTickPlayer = player
        }

        fun updateDRot() {
            lastDYaw = abs(player.prevYaw - player.yaw)
            lastDPitch = abs(player.prevPitch - player.pitch)
        }

        fun updateAccel(y: Float, p: Float) {
            lastYAccel = y
            lastPAccel = p
        }
    }
}

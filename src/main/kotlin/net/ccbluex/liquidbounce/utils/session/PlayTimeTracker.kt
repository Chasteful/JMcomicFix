package net.ccbluex.liquidbounce.utils.session

import net.ccbluex.liquidbounce.utils.client.mc

object PlayTimeTracker {
    private val playTimeMap = mutableMapOf<String, Long>()
    private var lastServer: String? = null
    private var lastUpdateTime: Long = System.currentTimeMillis()
    private var leftoverMs: Long = 0

    fun update() {
        val address = mc.currentServer?.ip ?: "singleplayer"
        val now = System.currentTimeMillis()
        val deltaMs = now - lastUpdateTime + leftoverMs

        if (deltaMs < 1000) {
            leftoverMs = deltaMs
            return
        }

        val deltaSeconds = deltaMs / 1000
        leftoverMs = deltaMs % 1000

        playTimeMap[address] = playTimeMap.getOrDefault(address, 0L) + deltaSeconds
        lastUpdateTime = now
        lastServer = address
    }

    fun getPlayTime(): Long {
        val address = mc.currentServer?.ip  ?: "singleplayer"
        return playTimeMap[address] ?: 0
    }
}

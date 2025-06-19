package net.ccbluex.jmcomicfix.utils.client.verificationUtils

import net.ccbluex.jmcomicfix.config.gson.serializer.HWIDUtils


class Verifications(
    val hwid: String = HWIDUtils.getHWID(),

    val developer: String = getDeveloperByHWID(HWIDUtils.getHWID())
) {
    companion object {
        private val ownerHWIDs = mapOf(
            "825ef7c1cd38ad8fffd571195e9f91f7e6da145245e5c8198189843191225d88" to "Chasteful"
        )

        private val developerHWIDs = mapOf(
            "d52275280bf2b780b915b9d2eb9636d1196049ca4d89eb6a3e6d7e8fdf5d10ab" to "5tvpidX1nx1n",
        )

        fun getDeveloperByHWID(hwid: String): String {
            return ownerHWIDs[hwid] ?: developerHWIDs[hwid] ?: "Customer"
        }
    }

    fun isOwner(): Boolean {
        return ownerHWIDs.containsKey(hwid)
    }

    fun isDeveloper(): Boolean {
        return developerHWIDs.containsKey(hwid)
    }
    fun getAvatarUrl(): String {
        return when {

            isOwner() -> "img/avatars/Owner.png"

            isDeveloper() -> "img/avatars/${developer}.png"

            else -> "img/avatars/Customer.png"
        }
    }
}


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
            "idk" to "5tvpidX1nx1n",
            "idk" to "KotlinModule"
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


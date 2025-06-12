package net.ccbluex.jmcomicfix.config.gson.serializer

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.ccbluex.jmcomicfix.utils.client.verificationUtils.Verifications
import java.lang.reflect.Type

object VerificationSerializer : JsonSerializer<Verifications> {
    override fun serialize(src: Verifications?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            src?.let {
                addProperty("isDev", it.isDeveloper())
                addProperty("isOwner", it.isOwner())
                addProperty("hwid", it.hwid)
                addProperty("developer", it.developer)
                addProperty("avatar", it.getAvatarUrl())

            }
        }
    }
}

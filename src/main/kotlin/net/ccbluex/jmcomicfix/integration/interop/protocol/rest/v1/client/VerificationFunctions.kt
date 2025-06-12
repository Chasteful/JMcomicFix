package net.ccbluex.jmcomicfix.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonElement
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.jmcomicfix.utils.client.verificationUtils.Verifications
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk

// GET /api/v1/client/verification
@Suppress("UNUSED_PARAMETER")
fun getVerification(requestObject: RequestObject): FullHttpResponse {
    val verifications = Verifications() // Creates new verification with auto-generated HWID
    val verificationInfo: JsonElement = interopGson.toJsonTree(verifications)
    return httpOk(verificationInfo)
}



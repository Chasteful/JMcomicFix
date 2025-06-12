package net.ccbluex.jmcomicfix.config.gson.serializer

import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.logging.Logger

object HWIDUtils {
    private val logger = Logger.getLogger(HWIDUtils::class.java.name)

    fun getHWID(): String {
        return try {
            val systemData = StringBuilder().apply {
                append(System.getProperty("os.name"))
                append(System.getProperty("os.arch"))
                append(System.getProperty("os.version"))
                append(System.getProperty("user.name"))
                append(System.getenv("PROCESSOR_IDENTIFIER"))
                append(System.getenv("PROCESSOR_ARCHITECTURE"))
                append(System.getenv("PROCESSOR_ARCHITEW6432"))
                append(System.getenv("NUMBER_OF_PROCESSORS"))

                NetworkInterface.getNetworkInterfaces()?.toList()
                    ?.firstOrNull { it.hardwareAddress != null && !it.isLoopback }
                    ?.hardwareAddress?.let {
                        append(it.joinToString(""))
                    }
            }.toString()

            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(systemData.toByteArray())

            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.warning("Failed to generate HWID: ${e.message}")
            "unknown-hwid-${System.currentTimeMillis()}"
        }
    }
}

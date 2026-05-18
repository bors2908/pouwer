package ge.becrin.pouwer.challenge.api

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

enum class PluginStatus {
    HEALTHY,
    STALE,
    UNHEALTHY
}

data class PluginRegistration(
    val id: String,
    val version: String,
    val contractVersion: String,
    val baseUrl: String
)

data class PluginHeartbeat(
    val id: String,
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    val timestamp: Instant,
    val status: String = "ok"
)

data class PluginMetadata(
    val id: String,
    val version: String,
    val contractVersion: String,
    val baseUrl: String,
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    val lastHeartbeat: Instant = Instant.now(),
    val status: PluginStatus = PluginStatus.HEALTHY,
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    val registeredAt: Instant = Instant.now()
)

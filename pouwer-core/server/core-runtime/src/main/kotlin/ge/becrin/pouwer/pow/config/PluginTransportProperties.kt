package ge.becrin.pouwer.pow.config

import ge.becrin.pouwer.challenge.api.PluginTransportMode
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pouwer.plugin-transport")
data class PluginTransportProperties(
    var mode: PluginTransportMode = PluginTransportMode.REST,
    var grpc: Grpc = Grpc()
) {
    data class Grpc(
        var port: Int = 9090,
        var schemaVersion: String = "v1",
        var requestTimeoutMs: Long = 30_000,
        var heartbeatLeaseMs: Long = 120_000
    )
}
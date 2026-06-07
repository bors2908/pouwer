package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.PluginTransportMode
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "plugin.core")
data class PluginBaseProperties(
    var url: String = "http://localhost:8082",
    var transport: PluginTransportMode = PluginTransportMode.REST,
    var grpc: Grpc = Grpc(),
    var heartbeat: Heartbeat = Heartbeat()
) {
    data class Grpc(
        var host: String = "localhost",
        var port: Int = 9090,
        var schemaVersion: String = "v1"
    )

    data class Heartbeat(
        var interval: Long = 30_000
    )
}

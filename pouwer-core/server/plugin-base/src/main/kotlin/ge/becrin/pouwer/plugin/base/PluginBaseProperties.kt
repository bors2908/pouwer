package ge.becrin.pouwer.plugin.base

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "plugin.core")
data class PluginBaseProperties(
    var url: String = "http://localhost:8082",
    var heartbeat: Heartbeat = Heartbeat()
) {
    data class Heartbeat(
        var interval: Long = 30_000
    )
}

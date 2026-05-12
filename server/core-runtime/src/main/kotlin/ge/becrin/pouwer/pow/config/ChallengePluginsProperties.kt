package ge.becrin.pouwer.pow.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

@ConfigurationProperties("challenge.plugins")
data class ChallengePluginsProperties(
    val mode: String = "serviceloader",
    val directory: String = "plugins"
) {
    fun resolvedMode(): PluginMode {
        return when (mode.lowercase()) {
            "serviceloader" -> PluginMode.SERVICELOADER
            "pf4j" -> PluginMode.PF4J
            else -> throw IllegalArgumentException(
                "Unsupported challenge.plugins.mode=$mode. Allowed values: serviceloader, pf4j"
            )
        }
    }

    fun pluginDirectoryPath(): Path = Paths.get(directory)
}

enum class PluginMode {
    SERVICELOADER,
    PF4J
}

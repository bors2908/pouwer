package ge.becrin.pouwer.pow.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

@ConfigurationProperties("challenge.plugins")
data class ChallengePluginsProperties(
    val directory: String = "plugins"
) {
    fun pluginDirectoryPath(): Path = Paths.get(directory)
}

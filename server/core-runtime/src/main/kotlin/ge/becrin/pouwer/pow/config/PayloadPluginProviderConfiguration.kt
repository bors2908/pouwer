package ge.becrin.pouwer.pow.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ge.becrin.pouwer.pow.service.PayloadPluginProvider
import ge.becrin.pouwer.pow.service.Pf4jPluginProvider
import ge.becrin.pouwer.pow.service.ServiceLoaderPluginProvider

@Configuration
open class PayloadPluginProviderConfiguration {

    @Bean
    open fun payloadPluginProvider(
        pluginsProperties: ChallengePluginsProperties
    ): PayloadPluginProvider {
        return when (pluginsProperties.resolvedMode()) {
            PluginMode.SERVICELOADER -> ServiceLoaderPluginProvider()
            PluginMode.PF4J -> Pf4jPluginProvider(pluginsProperties.pluginDirectoryPath())
        }
    }
}

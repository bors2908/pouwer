package ru.itmo.enterprise.pow.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.itmo.enterprise.pow.service.PayloadPluginProvider
import ru.itmo.enterprise.pow.service.Pf4jPluginProvider
import ru.itmo.enterprise.pow.service.ServiceLoaderPluginProvider

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

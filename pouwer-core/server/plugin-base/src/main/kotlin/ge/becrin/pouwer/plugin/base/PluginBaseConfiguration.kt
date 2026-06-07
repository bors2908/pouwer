package ge.becrin.pouwer.plugin.base

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PluginBaseProperties::class)
class PluginBaseConfiguration {
    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate = builder.build()

    @Bean
    fun grpcPluginClient(
        properties: PluginBaseProperties,
        payloadPlugin: PluginPayloadService,
        mapper: tools.jackson.databind.ObjectMapper
    ): GrpcPluginClient = GrpcPluginClient(properties, payloadPlugin, mapper)
}

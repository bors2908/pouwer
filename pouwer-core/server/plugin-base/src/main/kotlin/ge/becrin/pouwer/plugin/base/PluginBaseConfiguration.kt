package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.GRPC_VALUE
import ge.becrin.pouwer.challenge.api.REST_VALUE
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PluginBaseProperties::class)
class PluginBaseConfiguration {
    @Bean
    fun pluginConnectivityCoordinator(
        descriptor: PluginDescriptor,
        adapter: PluginCoreTransportAdapter
    ): PluginConnectivityCoordinator = PluginConnectivityCoordinator(descriptor, adapter)

    @Bean
    fun pluginTransportModeLogger(properties: PluginBaseProperties): PluginTransportModeLogger =
        PluginTransportModeLogger(properties)

    @Configuration
    @ConditionalOnProperty(
        prefix = "plugin.core",
        name = ["transport"],
        havingValue = REST_VALUE,
        matchIfMissing = true
    )
    class RestPluginBaseConfiguration {
        @Bean
        fun restTemplate(builder: RestTemplateBuilder): RestTemplate = builder.build()

        @Bean
        fun restPluginCoreTransportAdapter(
            properties: PluginBaseProperties,
            restTemplate: RestTemplate
        ): PluginCoreTransportAdapter = RestPluginCoreTransportAdapter(properties, restTemplate)
    }

    @Configuration
    @ConditionalOnProperty(
        prefix = "plugin.core",
        name = ["transport"],
        havingValue = GRPC_VALUE
    )
    class GrpcPluginBaseConfiguration {
        @Bean
        fun grpcPluginClient(
            properties: PluginBaseProperties,
            descriptor: PluginDescriptor,
            payloadPlugin: PluginPayloadService,
            mapper: ObjectMapper
        ): GrpcPluginClient = GrpcPluginClient(properties, descriptor, payloadPlugin, mapper)
    }

    class PluginTransportModeLogger(private val properties: PluginBaseProperties) {
        @EventListener(ApplicationReadyEvent::class)
        fun logTransportMode() {
            log.info { "Plugin transport mode: ${properties.transport.value}" }
        }

        companion object {
            private val log = KotlinLogging.logger {}
        }
    }
}

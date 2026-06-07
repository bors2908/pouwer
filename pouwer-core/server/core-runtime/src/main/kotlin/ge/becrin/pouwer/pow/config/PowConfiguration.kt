package ge.becrin.pouwer.pow.config

import ge.becrin.pouwer.challenge.api.GRPC_VALUE
import ge.becrin.pouwer.challenge.api.PluginTransport
import ge.becrin.pouwer.challenge.api.PluginTransportMode
import ge.becrin.pouwer.challenge.api.REST_VALUE
import ge.becrin.pouwer.pow.service.CircuitBreakerPluginTransport
import ge.becrin.pouwer.pow.service.CorePluginLifecycle
import ge.becrin.pouwer.pow.service.CorePluginLifecycleRegistry
import ge.becrin.pouwer.pow.service.PayloadPluginProvider
import ge.becrin.pouwer.pow.service.RemotePayloadPluginProvider
import ge.becrin.pouwer.pow.service.RestPluginTransport
import ge.becrin.pouwer.pow.service.TaskStore
import ge.becrin.pouwer.pow.service.grpc.CoreGrpcPluginStreamService
import ge.becrin.pouwer.pow.service.grpc.GrpcPluginConnectionStore
import ge.becrin.pouwer.pow.service.grpc.GrpcPluginEnvelopeValidator
import ge.becrin.pouwer.pow.service.grpc.GrpcPluginServerLifecycle
import ge.becrin.pouwer.pow.service.grpc.GrpcPluginTransport
import ge.becrin.pouwer.pow.service.store.InMemoryTaskStore
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PluginTransportProperties::class)
open class PowConfiguration {
    @Bean
    open fun taskStore(): TaskStore = InMemoryTaskStore()

    @Bean
    open fun corePluginLifecycleRegistry(): CorePluginLifecycleRegistry = CorePluginLifecycleRegistry()

    @Bean
    open fun circuitBreakerTransport(
        restTransport: ObjectProvider<RestPluginTransport>,
        grpcTransport: ObjectProvider<GrpcPluginTransport>,
        properties: PluginTransportProperties,
        lifecycle: CorePluginLifecycle,
        circuitBreakerRegistry: io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
    ): PluginTransport {
        val selected = when (properties.mode) {
            PluginTransportMode.GRPC -> grpcTransport.getObject()
            PluginTransportMode.REST -> restTransport.getObject()
        }
        return CircuitBreakerPluginTransport(selected, lifecycle, circuitBreakerRegistry)
    }

    @Bean
    open fun remotePluginProvider(
        lifecycle: CorePluginLifecycle,
        @Qualifier("circuitBreakerTransport")
        transport: PluginTransport
    ): PayloadPluginProvider = RemotePayloadPluginProvider(lifecycle, transport)

    @Bean
    open fun corePluginTransportModeLogger(properties: PluginTransportProperties): CorePluginTransportModeLogger =
        CorePluginTransportModeLogger(properties)

    @Configuration
    @ConditionalOnProperty(
        prefix = "pouwer.plugin-transport",
        name = ["mode"],
        havingValue = REST_VALUE,
        matchIfMissing = true
    )
    open class RestPluginTransportConfiguration {
        @Bean
        open fun restPluginTransport(): RestPluginTransport = RestPluginTransport()
    }

    @Configuration
    @ConditionalOnProperty(
        prefix = "pouwer.plugin-transport",
        name = ["mode"],
        havingValue = GRPC_VALUE
    )
    open class GrpcPluginTransportConfiguration {
        @Bean
        open fun grpcPluginConnectionStore(properties: PluginTransportProperties): GrpcPluginConnectionStore =
            GrpcPluginConnectionStore(properties.grpc.requestTimeoutMs, properties.grpc.heartbeatLeaseMs)

        @Bean
        open fun grpcPluginEnvelopeValidator(properties: PluginTransportProperties): GrpcPluginEnvelopeValidator =
            GrpcPluginEnvelopeValidator(properties.grpc.schemaVersion)

        @Bean
        open fun grpcPluginTransport(
            connectionStore: GrpcPluginConnectionStore,
            mapper: ObjectMapper,
            properties: PluginTransportProperties
        ): GrpcPluginTransport = GrpcPluginTransport(connectionStore, mapper, properties.grpc.schemaVersion)

        @Bean
        open fun grpcPluginServer(
            connectionStore: GrpcPluginConnectionStore,
            lifecycle: CorePluginLifecycle,
            validator: GrpcPluginEnvelopeValidator,
            properties: PluginTransportProperties
        ): Server = NettyServerBuilder.forPort(properties.grpc.port)
            .addService(CoreGrpcPluginStreamService(connectionStore, lifecycle, validator))
            .build()

        @Bean
        open fun grpcPluginServerLifecycle(server: Server): GrpcPluginServerLifecycle = GrpcPluginServerLifecycle(server)
    }

    class CorePluginTransportModeLogger(private val properties: PluginTransportProperties) {
        @EventListener(ApplicationReadyEvent::class)
        fun logTransportMode() {
            log.info { "Core plugin transport mode: ${properties.mode.value}" }
        }

        companion object {
            private val log = KotlinLogging.logger {}
        }
    }
}

package ge.becrin.pouwer.pow.config

import ge.becrin.pouwer.challenge.api.PluginTransport
import ge.becrin.pouwer.pow.service.CircuitBreakerPluginTransport
import ge.becrin.pouwer.pow.service.PayloadPluginProvider
import ge.becrin.pouwer.pow.service.RemotePayloadPluginProvider
import ge.becrin.pouwer.pow.service.RemotePluginRegistry
import ge.becrin.pouwer.pow.service.RestPluginTransport
import ge.becrin.pouwer.pow.service.TaskStore
import ge.becrin.pouwer.pow.service.grpc.CoreGrpcPluginStreamService
import ge.becrin.pouwer.pow.service.grpc.GrpcPluginConnectionRegistry
import ge.becrin.pouwer.pow.service.grpc.GrpcPluginEnvelopeValidator
import ge.becrin.pouwer.pow.service.grpc.GrpcPluginServerLifecycle
import ge.becrin.pouwer.pow.service.grpc.GrpcPluginTransport
import ge.becrin.pouwer.pow.service.store.InMemoryTaskStore
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PluginTransportProperties::class)
open class PowConfiguration {

    @Bean
    open fun taskStore(): TaskStore = InMemoryTaskStore()

    @Bean
    open fun remotePluginRegistry(): RemotePluginRegistry = RemotePluginRegistry()

    @Bean
    open fun restPluginTransport(): RestPluginTransport = RestPluginTransport()

    @Bean
    @ConditionalOnProperty(prefix = "pouwer.plugin-transport", name = ["mode"], havingValue = "grpc")
    open fun grpcPluginConnectionRegistry(properties: PluginTransportProperties): GrpcPluginConnectionRegistry =
        GrpcPluginConnectionRegistry(properties.grpc.requestTimeoutMs, properties.grpc.heartbeatLeaseMs)

    @Bean
    @ConditionalOnProperty(prefix = "pouwer.plugin-transport", name = ["mode"], havingValue = "grpc")
    open fun grpcPluginEnvelopeValidator(properties: PluginTransportProperties): GrpcPluginEnvelopeValidator =
        GrpcPluginEnvelopeValidator(properties.grpc.schemaVersion)

    @Bean
    @ConditionalOnProperty(prefix = "pouwer.plugin-transport", name = ["mode"], havingValue = "grpc")
    open fun grpcPluginTransport(
        registry: GrpcPluginConnectionRegistry,
        mapper: ObjectMapper,
        properties: PluginTransportProperties
    ): GrpcPluginTransport = GrpcPluginTransport(registry, mapper, properties.grpc.schemaVersion)

    @Bean
    @ConditionalOnProperty(prefix = "pouwer.plugin-transport", name = ["mode"], havingValue = "grpc")
    open fun grpcPluginServer(
        registry: GrpcPluginConnectionRegistry,
        validator: GrpcPluginEnvelopeValidator,
        properties: PluginTransportProperties
    ): Server = NettyServerBuilder.forPort(properties.grpc.port)
        .addService(CoreGrpcPluginStreamService(registry, validator))
        .build()

    @Bean
    @ConditionalOnProperty(prefix = "pouwer.plugin-transport", name = ["mode"], havingValue = "grpc")
    open fun grpcPluginServerLifecycle(server: Server): GrpcPluginServerLifecycle = GrpcPluginServerLifecycle(server)

    @Bean
    open fun circuitBreakerTransport(
        restTransport: RestPluginTransport,
        grpcTransport: org.springframework.beans.factory.ObjectProvider<GrpcPluginTransport>,
        properties: PluginTransportProperties,
        registry: RemotePluginRegistry,
        circuitBreakerRegistry: io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
    ): PluginTransport {
        val selected = if (properties.mode.equals("grpc", ignoreCase = true)) {
            grpcTransport.getObject()
        } else {
            restTransport
        }
        return CircuitBreakerPluginTransport(selected, registry, circuitBreakerRegistry)
    }

    @Bean
    open fun remotePluginProvider(
        registry: RemotePluginRegistry,
        @Qualifier("circuitBreakerTransport")
        transport: PluginTransport
    ): PayloadPluginProvider = RemotePayloadPluginProvider(registry, transport)
}

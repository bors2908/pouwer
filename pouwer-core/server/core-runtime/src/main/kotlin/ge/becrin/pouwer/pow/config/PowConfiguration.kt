package ge.becrin.pouwer.pow.config

import ge.becrin.pouwer.challenge.api.PluginTransport
import ge.becrin.pouwer.pow.service.CircuitBreakerPluginTransport
import ge.becrin.pouwer.pow.service.PayloadPluginProvider
import ge.becrin.pouwer.pow.service.RemotePayloadPluginProvider
import ge.becrin.pouwer.pow.service.RemotePluginRegistry
import ge.becrin.pouwer.pow.service.RestPluginTransport
import ge.becrin.pouwer.pow.service.TaskStore
import ge.becrin.pouwer.pow.service.store.InMemoryTaskStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
open class PowConfiguration {

    @Bean
    open fun taskStore(): TaskStore = InMemoryTaskStore()

    @Bean
    open fun remotePluginRegistry(): RemotePluginRegistry = RemotePluginRegistry()

    @Bean
    open fun restPluginTransport(): RestPluginTransport = RestPluginTransport()

    @Bean
    open fun circuitBreakerTransport(
        restTransport: RestPluginTransport,
        registry: RemotePluginRegistry,
        circuitBreakerRegistry: io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
    ): PluginTransport = CircuitBreakerPluginTransport(restTransport, registry, circuitBreakerRegistry)

    @Bean
    open fun remotePluginProvider(
        registry: RemotePluginRegistry,
        @Qualifier("circuitBreakerTransport")
        transport: PluginTransport
    ): PayloadPluginProvider = RemotePayloadPluginProvider(registry, transport)
}

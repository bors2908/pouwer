package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.PluginHeartbeat
import ge.becrin.pouwer.challenge.api.PluginRegistration
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import java.time.Instant

class PluginConnectivityCoordinator(
    private val descriptor: PluginDescriptor,
    private val adapter: PluginCoreTransportAdapter
) {
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        adapter.start()
        val registration = PluginRegistration(
            id = descriptor.pluginId,
            version = descriptor.version,
            contractVersion = CHALLENGE_PLUGIN_CONTRACT_VERSION,
            baseUrl = descriptor.baseUrl
        )
        adapter.register(registration)
        log.info { "Registered plugin ${descriptor.pluginId} at ${registration.baseUrl}" }
    }

    @Scheduled(fixedDelayString = $$"${plugin.core.heartbeat.interval:30000}")
    fun sendHeartbeat() {
        val heartbeat = PluginHeartbeat(
            id = descriptor.pluginId,
            timestamp = Instant.now()
        )
        adapter.heartbeat(heartbeat)
        log.debug { "Heartbeat sent for ${descriptor.pluginId}" }
    }

    @PreDestroy
    fun stop() {
        adapter.unregister(descriptor.pluginId)
        adapter.stop()
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
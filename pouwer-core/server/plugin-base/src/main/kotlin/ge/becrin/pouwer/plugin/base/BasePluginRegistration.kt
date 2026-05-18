package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.PluginHeartbeat
import ge.becrin.pouwer.challenge.api.PluginRegistration
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.time.Instant

abstract class BasePluginRegistration(
    private val properties: PluginBaseProperties,
    @param:Value($$"${spring.application.version}")
    private val pluginVersion: String,
    private val httpClient: RestTemplate
) {
    protected abstract fun getPluginId(): String
    protected abstract fun getPluginPort(): Int
    protected abstract fun getPluginHost(): String

    protected open fun getBaseUrl(): String = "${getPluginHost()}:${getPluginPort()}"

    @EventListener(ApplicationReadyEvent::class)
    fun registerPlugin() {
        val pluginId = getPluginId()
        val registration = PluginRegistration(
            id = pluginId,
            version = pluginVersion,
            contractVersion = CHALLENGE_PLUGIN_CONTRACT_VERSION,
            baseUrl = getBaseUrl()
        )

        val response = httpClient.postForEntity<String>(
            "${properties.url}/core/plugins/register",
            registration
        )

        if (!response.statusCode.is2xxSuccessful) {
            throw IllegalStateException("Plugin registration failed for $pluginId with ${response.statusCode}")
        }

        log.info { "Registered plugin $pluginId at ${registration.baseUrl}" }
    }

    @Scheduled(fixedDelayString = $$"${plugin.core.heartbeat.interval:30000}")
    fun sendHeartbeat() {
        val pluginId = getPluginId()
        val heartbeat = PluginHeartbeat(
            id = pluginId,
            timestamp = Instant.now()
        )

        val response = httpClient.postForEntity<String>(
            "${properties.url}/core/plugins/$pluginId/heartbeat",
            heartbeat
        )

        if (!response.statusCode.is2xxSuccessful) {
            log.error {
                "Heartbeat rejected for $pluginId with ${response.statusCode} from ${properties.url}"
            }
            throw IllegalStateException("Heartbeat rejected for $pluginId with ${response.statusCode}")
        }

        log.debug { "Heartbeat sent for $pluginId" }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

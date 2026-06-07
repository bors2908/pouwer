package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.PluginHeartbeat
import ge.becrin.pouwer.challenge.api.PluginRegistration
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

class RestPluginCoreTransportAdapter(
    private val properties: PluginBaseProperties,
    private val httpClient: RestTemplate
) : PluginCoreTransportAdapter {
    override fun register(registration: PluginRegistration) {
        val response = httpClient.postForEntity<String>(
            "${properties.url}/core/plugins/register",
            registration
        )

        if (!response.statusCode.is2xxSuccessful) {
            throw IllegalStateException("Plugin registration failed for ${registration.id} with ${response.statusCode}")
        }
    }

    override fun heartbeat(heartbeat: PluginHeartbeat) {
        val response = httpClient.postForEntity<String>(
            "${properties.url}/core/plugins/${heartbeat.id}/heartbeat",
            heartbeat
        )

        if (!response.statusCode.is2xxSuccessful) {
            throw IllegalStateException("Heartbeat rejected for ${heartbeat.id} with ${response.statusCode}")
        }
    }

    override fun unregister(pluginId: String) {
        httpClient.delete("${properties.url}/core/plugins/$pluginId")
    }
}
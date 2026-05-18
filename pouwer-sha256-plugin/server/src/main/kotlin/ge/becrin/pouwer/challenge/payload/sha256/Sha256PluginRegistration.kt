package ge.becrin.pouwer.challenge.payload.sha256

import ge.becrin.pouwer.plugin.base.BasePluginRegistration
import ge.becrin.pouwer.plugin.base.PluginBaseProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class Sha256PluginRegistration(
    properties: PluginBaseProperties,
    @Value($$"${spring.application.version}")
    pluginVersion: String,
    private val payloadPlugin: Sha256PayloadPlugin,
    @param:Value($$"${server.port}")
    private val pluginPort: Int,
    @param:Value($$"${plugin.host}")
    private val pluginHost: String,
    httpClient: RestTemplate
) : BasePluginRegistration(properties, pluginVersion, httpClient) {

    override fun getPluginId(): String = payloadPlugin.pluginId
    override fun getPluginPort(): Int = pluginPort
    override fun getPluginHost(): String = pluginHost
}

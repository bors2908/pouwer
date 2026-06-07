package ge.becrin.pouwer.challenge.payload.bitcoin

import ge.becrin.pouwer.plugin.base.BasePluginRegistration
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BitcoinPluginRegistration(
    @Value($$"${spring.application.version}")
    pluginVersion: String,
    private val payloadPlugin: BitcoinRpcPayloadPlugin,
    @param:Value($$"${server.port}")
    private val pluginPort: Int,
    @param:Value($$"${plugin.host}")
    private val pluginHost: String
) : BasePluginRegistration(pluginVersion) {

    override val pluginId: String
        get() = payloadPlugin.pluginId
    override val port: Int
        get() = pluginPort
    override val host: String
        get() = pluginHost
}

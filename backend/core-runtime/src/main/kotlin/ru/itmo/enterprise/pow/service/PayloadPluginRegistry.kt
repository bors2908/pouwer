package ru.itmo.enterprise.pow.service

import org.springframework.stereotype.Component
import ru.itmo.enterprise.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ru.itmo.enterprise.challenge.api.ContractMismatchException
import ru.itmo.enterprise.challenge.api.DuplicatePluginIdException
import ru.itmo.enterprise.challenge.api.PayloadPlugin
import ru.itmo.enterprise.challenge.api.UnsupportedPluginException

@Component
class PayloadPluginRegistry(
    plugins: Collection<PayloadPlugin>
) {
    private val byId: Map<String, PayloadPlugin>

    init {
        val map = linkedMapOf<String, PayloadPlugin>()
        plugins.forEach { plugin ->
            if (plugin.contractVersion != CHALLENGE_PLUGIN_CONTRACT_VERSION) {
                throw ContractMismatchException(
                    pluginId = plugin.pluginId,
                    expected = CHALLENGE_PLUGIN_CONTRACT_VERSION,
                    actual = plugin.contractVersion
                )
            }
            if (map.put(plugin.pluginId, plugin) != null) {
                throw DuplicatePluginIdException(plugin.pluginId)
            }
        }
        byId = map.toMap()
    }

    fun get(pluginId: String): PayloadPlugin {
        return byId[pluginId] ?: throw UnsupportedPluginException(pluginId, byId.keys)
    }

    fun find(pluginId: String): PayloadPlugin? = byId[pluginId]

    fun pluginIds(): Set<String> = byId.keys
}

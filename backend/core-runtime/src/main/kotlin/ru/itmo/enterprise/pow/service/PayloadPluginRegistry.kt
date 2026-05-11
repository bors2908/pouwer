package ru.itmo.enterprise.pow.service

import org.springframework.stereotype.Component
import ru.itmo.enterprise.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ru.itmo.enterprise.challenge.api.ContractMismatchException
import ru.itmo.enterprise.challenge.api.DuplicatePluginIdException
import ru.itmo.enterprise.challenge.api.PayloadPlugin
import ru.itmo.enterprise.challenge.api.UnsupportedPluginException
import java.util.ServiceLoader

@Component
class PayloadPluginRegistry private constructor(
    private val byId: Map<String, PayloadPlugin>
) {
    constructor() : this(buildRegistry(loadPluginsFromClasspath()))

    internal constructor(plugins: List<PayloadPlugin>) : this(buildRegistry(plugins))

    fun get(pluginId: String): PayloadPlugin {
        return byId[pluginId] ?: throw UnsupportedPluginException(pluginId, byId.keys)
    }

    fun find(pluginId: String): PayloadPlugin? = byId[pluginId]

    fun pluginIds(): Set<String> = byId.keys

    companion object {
        private fun loadPluginsFromClasspath(): List<PayloadPlugin> {
            return ServiceLoader.load(PayloadPlugin::class.java)
                .iterator()
                .asSequence()
                .toList()
        }

        private fun buildRegistry(plugins: List<PayloadPlugin>): Map<String, PayloadPlugin> {
            val map = linkedMapOf<String, PayloadPlugin>()
            plugins
                .sortedBy { it.id() }
                .forEach { plugin ->
                    if (plugin.contractVersion != CHALLENGE_PLUGIN_CONTRACT_VERSION) {
                        throw ContractMismatchException(
                            pluginId = plugin.id(),
                            expected = CHALLENGE_PLUGIN_CONTRACT_VERSION,
                            actual = plugin.contractVersion
                        )
                    }
                    if (map.put(plugin.id(), plugin) != null) {
                        throw DuplicatePluginIdException(plugin.id())
                    }
                }
            return map.toMap()
        }
    }
}

package ru.itmo.enterprise.pow.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.itmo.enterprise.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ru.itmo.enterprise.challenge.api.ContractMismatchException
import ru.itmo.enterprise.challenge.api.DuplicatePluginIdException
import ru.itmo.enterprise.challenge.api.PayloadPlugin
import ru.itmo.enterprise.challenge.api.UnsupportedPluginException

@Component
class PayloadPluginRegistry @Autowired constructor(
    private val pluginProvider: PayloadPluginProvider
) {
    @Volatile
    private var byId: Map<String, PayloadPlugin> = emptyMap()

    constructor(plugins: List<PayloadPlugin>) : this(StaticPayloadPluginProvider(plugins))

    init {
        refresh()
    }

    fun get(pluginId: String): PayloadPlugin {
        return byId[pluginId] ?: throw UnsupportedPluginException(pluginId, byId.keys)
    }

    fun find(pluginId: String): PayloadPlugin? = byId[pluginId]

    fun pluginIds(): Set<String> = byId.keys

    @Synchronized
    fun refresh() {
        byId = buildRegistry(pluginProvider.loadPlugins())
    }

    @Synchronized
    fun disable(pluginId: String, cause: Throwable? = null) {
        if (!byId.containsKey(pluginId)) {
            return
        }
        pluginProvider.disable(pluginId)
        byId = byId - pluginId
        if (cause != null) {
            log.error(cause) { "Disabled plugin $pluginId due to runtime failure" }
        } else {
            log.warn { "Disabled plugin $pluginId" }
        }
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

    private class StaticPayloadPluginProvider(
        private val plugins: List<PayloadPlugin>
    ) : PayloadPluginProvider {
        override fun loadPlugins(): List<PayloadPlugin> = plugins
        override fun disable(pluginId: String) = Unit
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

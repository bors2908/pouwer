package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.ContractMismatchException
import ge.becrin.pouwer.challenge.api.DuplicatePluginIdException
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.challenge.api.UnsupportedPluginException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PayloadPluginRegistry
@Autowired constructor(
    private val pluginProvider: PayloadPluginProvider
) {
    private val plugins = PluginStateRegistry<PayloadPlugin>()

    constructor(plugins: List<PayloadPlugin>) : this(StaticPayloadPluginProvider(plugins))

    init {
        refresh()
    }

    fun get(pluginId: String): PayloadPlugin {
        return plugins.get(pluginId) ?: throw UnsupportedPluginException(pluginId, plugins.pluginIds())
    }

    fun find(pluginId: String): PayloadPlugin? = plugins.get(pluginId)

    fun pluginIds(): Set<String> = plugins.pluginIds()

    @Synchronized
    fun refresh() {
        plugins.replaceAll(buildRegistry(pluginProvider.loadPlugins()))
    }

    @Scheduled(
        fixedDelayString = $$"${challenge.plugins.refresh-interval:5000}",
        initialDelayString = $$"${challenge.plugins.refresh-initial-delay:5000}"
    )
    fun scheduledRefresh() {
        refresh()
    }

    @Synchronized
    fun disable(pluginId: String, cause: Throwable? = null) {
        if (plugins.remove(pluginId) == null) {
            return
        }
        pluginProvider.disable(pluginId)
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

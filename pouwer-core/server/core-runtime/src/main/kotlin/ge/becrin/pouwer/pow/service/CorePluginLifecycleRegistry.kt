package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginRegistration
import ge.becrin.pouwer.challenge.api.PluginStatus
import ge.becrin.pouwer.challenge.api.PluginTransportMode
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

interface CorePluginLifecycle {
    fun register(registration: PluginRegistration, source: PluginConnectionSource): PluginMetadata
    fun register(metadata: PluginMetadata): PluginMetadata
    fun heartbeat(pluginId: String, timestamp: Instant, source: PluginConnectionSource): Boolean
    fun heartbeat(pluginId: String, timestamp: Instant): Boolean
    fun get(pluginId: String): PluginMetadata?
    fun getAll(): List<PluginMetadata>
    fun healthyPlugins(): List<PluginMetadata>
    fun getHealthy(): List<PluginMetadata>
    fun evictStale(): Int
    fun markUnavailable(pluginId: String, cause: Throwable? = null): Boolean
    fun markUnhealthy(pluginId: String): Boolean
    fun unregister(pluginId: String, source: PluginConnectionSource): Boolean
    fun unregister(pluginId: String): Boolean
    fun clear()
}

data class PluginConnectionSource(
    val mode: PluginTransportMode,
    val sessionId: String? = null,
    val baseUrl: String? = null
)

class CorePluginLifecycleRegistry(
    private val keepaliveIntervalMs: Long = 30_000,
    private val evictionThresholdMs: Long = 120_000
): CorePluginLifecycle {
    private val plugins = PluginStateRegistry<PluginMetadata>()

    override fun register(registration: PluginRegistration, source: PluginConnectionSource): PluginMetadata {
        val now = Instant.now()
        val metadata = PluginMetadata(
            id = registration.id,
            version = registration.version,
            contractVersion = registration.contractVersion,
            baseUrl = source.baseUrl ?: registration.baseUrl,
            registeredAt = now,
            lastHeartbeat = now,
            status = PluginStatus.HEALTHY
        )
        return register(metadata)
    }

    @Synchronized
    override fun register(metadata: PluginMetadata): PluginMetadata {
        val existing = plugins.get(metadata.id)
        if (existing != null) {
            val updated = metadata.copy(registeredAt = existing.registeredAt)
            plugins.replace(metadata.id, updated)
            log.info { "Re-registered plugin ${metadata.id} at ${metadata.baseUrl}" }
            return updated
        }

        plugins.replace(metadata.id, metadata)
        log.info { "Registered plugin ${metadata.id} ${metadata.version} at ${metadata.baseUrl}" }
        return metadata
    }

    override fun heartbeat(pluginId: String, timestamp: Instant, source: PluginConnectionSource): Boolean =
        heartbeat(pluginId, timestamp)

    @Synchronized
    override fun heartbeat(pluginId: String, timestamp: Instant): Boolean {
        val plugin = plugins.get(pluginId) ?: return false
        val updated = plugin.copy(
            lastHeartbeat = timestamp,
            status = PluginStatus.HEALTHY
        )
        plugins.replace(pluginId, updated)
        return true
    }

    override fun get(pluginId: String): PluginMetadata? = plugins.get(pluginId)

    override fun getAll(): List<PluginMetadata> = plugins.values()

    override fun healthyPlugins(): List<PluginMetadata> = plugins.values().filter { it.status == PluginStatus.HEALTHY }

    override fun getHealthy(): List<PluginMetadata> = healthyPlugins()

    @Synchronized
    override fun evictStale(): Int {
        val now = Instant.now()
        val staleIds = plugins.values()
            .filter { metadata -> metadata.lastHeartbeat.toEpochMilli() + evictionThresholdMs < now.toEpochMilli() }
            .map { it.id }

        staleIds.forEach { id ->
            plugins.remove(id)
            log.warn { "Evicted stale plugin $id (no heartbeat for ${evictionThresholdMs}ms)" }
        }

        return staleIds.size
    }

    override fun markUnavailable(pluginId: String, cause: Throwable?): Boolean = markUnhealthy(pluginId)

    @Synchronized
    override fun markUnhealthy(pluginId: String): Boolean {
        val plugin = plugins.get(pluginId) ?: return false
        plugins.replace(pluginId, plugin.copy(status = PluginStatus.UNHEALTHY))
        log.warn { "Marked plugin $pluginId as unhealthy" }
        return true
    }

    override fun unregister(pluginId: String, source: PluginConnectionSource): Boolean = unregister(pluginId)

    @Synchronized
    override fun unregister(pluginId: String): Boolean {
        val removed = plugins.remove(pluginId) != null
        if (removed) {
            log.info { "Unregistered plugin $pluginId" }
        }
        return removed
    }

    @Synchronized
    override fun clear() {
        val count = plugins.pluginIds().size
        plugins.replaceAll(emptyMap())
        log.info { "Cleared registry ($count plugins removed)" }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

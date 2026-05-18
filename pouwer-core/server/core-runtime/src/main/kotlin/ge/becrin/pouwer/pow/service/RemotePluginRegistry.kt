package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

class RemotePluginRegistry(
    private val keepaliveIntervalMs: Long = 30_000,
    private val evictionThresholdMs: Long = 120_000
) {
    private val plugins = ConcurrentHashMap<String, PluginMetadata>()
    private val lock = ReentrantReadWriteLock()

    fun register(metadata: PluginMetadata): PluginMetadata {
        lock.writeLock().lock()
        try {
            val existing = plugins[metadata.id]
            if (existing != null) {
                val updated = metadata.copy(registeredAt = existing.registeredAt)
                plugins[metadata.id] = updated
                log.info { "Re-registered plugin ${metadata.id} at ${metadata.baseUrl}" }
                return updated
            }

            plugins[metadata.id] = metadata
            log.info { "Registered plugin ${metadata.id} ${metadata.version} at ${metadata.baseUrl}" }
            return metadata
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun heartbeat(pluginId: String, timestamp: Instant): Boolean {
        lock.writeLock().lock()
        try {
            val plugin = plugins[pluginId] ?: return false
            val updated = plugin.copy(
                lastHeartbeat = timestamp,
                status = PluginStatus.HEALTHY
            )
            plugins[pluginId] = updated
            return true
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun get(pluginId: String): PluginMetadata? {
        lock.readLock().lock()
        try {
            return plugins[pluginId]
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getAll(): List<PluginMetadata> {
        lock.readLock().lock()
        try {
            return plugins.values.toList()
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getHealthy(): List<PluginMetadata> {
        lock.readLock().lock()
        try {
            return plugins.values.filter { it.status == PluginStatus.HEALTHY }.toList()
        } finally {
            lock.readLock().unlock()
        }
    }

    fun evictStale(): Int {
        lock.writeLock().lock()
        try {
            val now = Instant.now()
            val staleIds = plugins.filter { (_, metadata) ->
                metadata.lastHeartbeat.toEpochMilli() + evictionThresholdMs < now.toEpochMilli()
            }.keys

            staleIds.forEach { id ->
                plugins.remove(id)
                log.warn { "Evicted stale plugin $id (no heartbeat for ${evictionThresholdMs}ms)" }
            }

            return staleIds.size
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun markUnhealthy(pluginId: String): Boolean {
        lock.writeLock().lock()
        try {
            val plugin = plugins[pluginId] ?: return false
            plugins[pluginId] = plugin.copy(status = PluginStatus.UNHEALTHY)
            log.warn { "Marked plugin $pluginId as unhealthy" }
            return true
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun unregister(pluginId: String): Boolean {
        lock.writeLock().lock()
        try {
            val removed = plugins.remove(pluginId) != null
            if (removed) {
                log.info { "Unregistered plugin $pluginId" }
            }
            return removed
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun clear() {
        lock.writeLock().lock()
        try {
            val count = plugins.size
            plugins.clear()
            log.info { "Cleared registry ($count plugins removed)" }
        } finally {
            lock.writeLock().unlock()
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

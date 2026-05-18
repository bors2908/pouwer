package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemotePluginRegistryTest {
    private lateinit var registry: RemotePluginRegistry

    @BeforeEach
    fun setUp() {
        registry = RemotePluginRegistry(
            keepaliveIntervalMs = 30_000,
            evictionThresholdMs = 60_000
        )
    }

    @Test
    fun testRegisterPlugin() {
        val metadata = createTestMetadata("plugin1", "http://localhost:8081")

        val registered = registry.register(metadata)

        assertEquals("plugin1", registered.id)
        assertEquals(PluginStatus.HEALTHY, registered.status)
        assertNotNull(registered.registeredAt)
    }

    @Test
    fun testGetPlugin() {
        val metadata = createTestMetadata("plugin1", "http://localhost:8081")
        registry.register(metadata)

        val retrieved = registry.get("plugin1")

        assertNotNull(retrieved)
        assertEquals("plugin1", retrieved.id)
    }

    @Test
    fun testGetNonExistentPlugin() {
        val retrieved = registry.get("nonexistent")
        assertNull(retrieved)
    }

    @Test
    fun testGetAll() {
        registry.register(createTestMetadata("plugin1", "http://localhost:8081"))
        registry.register(createTestMetadata("plugin2", "http://localhost:8082"))

        val all = registry.getAll()

        assertEquals(2, all.size)
    }

    @Test
    fun testHeartbeat() {
        val metadata = createTestMetadata("plugin1", "http://localhost:8081")
        registry.register(metadata)

        val timestamp = Instant.now()
        val success = registry.heartbeat("plugin1", timestamp)

        assertTrue(success)
        val updated = registry.get("plugin1")
        assertNotNull(updated)
        assertEquals(timestamp, updated.lastHeartbeat)
        assertEquals(PluginStatus.HEALTHY, updated.status)
    }

    @Test
    fun testHeartbeatNonExistentPlugin() {
        val success = registry.heartbeat("nonexistent", Instant.now())
        assertFalse(success)
    }

    @Test
    fun testMarkUnhealthy() {
        val metadata = createTestMetadata("plugin1", "http://localhost:8081")
        registry.register(metadata)

        val success = registry.markUnhealthy("plugin1")

        assertTrue(success)
        val updated = registry.get("plugin1")
        assertNotNull(updated)
        assertEquals(PluginStatus.UNHEALTHY, updated.status)
    }

    @Test
    fun testUnregister() {
        val metadata = createTestMetadata("plugin1", "http://localhost:8081")
        registry.register(metadata)

        val removed = registry.unregister("plugin1")

        assertTrue(removed)
        assertNull(registry.get("plugin1"))
    }

    @Test
    fun testUnregisterNonExistent() {
        val removed = registry.unregister("nonexistent")
        assertFalse(removed)
    }

    @Test
    fun testEvictStalePlugins() {
        val registry = RemotePluginRegistry(
            keepaliveIntervalMs = 30_000,
            evictionThresholdMs = 1_000  // 1 second threshold for testing
        )

        val metadata1 = createTestMetadata("plugin1", "http://localhost:8081")
            .copy(lastHeartbeat = Instant.now().minusSeconds(5))
        registry.register(metadata1)

        val metadata2 = createTestMetadata("plugin2", "http://localhost:8082")
        registry.register(metadata2)

        Thread.sleep(1_500)  // Wait for eviction threshold
        registry.heartbeat("plugin2", Instant.now())

        val evicted = registry.evictStale()

        assertEquals(1, evicted)
        assertNull(registry.get("plugin1"))
        assertNotNull(registry.get("plugin2"))
    }

    @Test
    fun testGetHealthy() {
        registry.register(createTestMetadata("plugin1", "http://localhost:8081"))
        registry.register(createTestMetadata("plugin2", "http://localhost:8082"))

        registry.markUnhealthy("plugin2")

        val healthy = registry.getHealthy()

        assertEquals(1, healthy.size)
        assertEquals("plugin1", healthy[0].id)
    }

    @Test
    fun testClear() {
        registry.register(createTestMetadata("plugin1", "http://localhost:8081"))
        registry.register(createTestMetadata("plugin2", "http://localhost:8082"))

        registry.clear()

        assertEquals(0, registry.getAll().size)
    }

    @Test
    fun testReRegisterPlugin() {
        val metadata1 = createTestMetadata("plugin1", "http://localhost:8081")
        val registered1 = registry.register(metadata1)

        Thread.sleep(100)

        val metadata2 = createTestMetadata("plugin1", "http://localhost:8081")
        val registered2 = registry.register(metadata2)

        assertEquals(1, registry.getAll().size)
        assertEquals(registered1.registeredAt, registered2.registeredAt)
    }

    private fun createTestMetadata(id: String, baseUrl: String): PluginMetadata {
        return PluginMetadata(
            id = id,
            version = "1.0.0",
            contractVersion = "1.0.0",
            baseUrl = baseUrl,
            lastHeartbeat = Instant.now(),
            status = PluginStatus.HEALTHY,
            registeredAt = Instant.now()
        )
    }
}

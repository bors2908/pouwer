package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginStatus
import ge.becrin.pouwer.challenge.api.PluginTransport
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RemotePayloadPluginSystemIntegrationTest {
    private lateinit var registry: RemotePluginRegistry
    private lateinit var mockTransport: MockPluginTransport
    private lateinit var provider: RemotePayloadPluginProvider

    @BeforeEach
    fun setUp() {
        registry = RemotePluginRegistry(
            keepaliveIntervalMs = 30_000,
            evictionThresholdMs = 60_000
        )
        mockTransport = MockPluginTransport()
        provider = RemotePayloadPluginProvider(registry, mockTransport)
    }

    @Test
    fun testPluginRegistrationAndLoading() {
        val metadata = createTestMetadata("test-plugin", "http://localhost:8081")
        registry.register(metadata)

        val plugins = provider.loadPlugins()

        assertEquals(1, plugins.size)
        assertEquals("test-plugin", plugins[0].id())
    }

    @Test
    fun testMultiplePluginsLoad() {
        registry.register(createTestMetadata("plugin-1", "http://localhost:8081"))
        registry.register(createTestMetadata("plugin-2", "http://localhost:8082"))
        registry.register(createTestMetadata("plugin-3", "http://localhost:8083"))

        val plugins = provider.loadPlugins()

        assertEquals(3, plugins.size)
    }

    @Test
    fun testUnhealthyPluginsAreFiltered() {
        registry.register(createTestMetadata("plugin-healthy", "http://localhost:8081"))
        registry.register(createTestMetadata("plugin-unhealthy", "http://localhost:8082"))

        registry.markUnhealthy("plugin-unhealthy")

        val plugins = provider.loadPlugins()

        assertEquals(1, plugins.size)
        assertEquals("plugin-healthy", plugins[0].id())
    }

    @Test
    @Timeout(3)
    fun testHeartbeatKeepsPluginAlive() {
        val metadata = createTestMetadata("test-plugin", "http://localhost:8081")
        registry.register(metadata)

        for (i in 0..5) {
            registry.heartbeat("test-plugin", Instant.now())
            Thread.sleep(100)
        }

        val plugins = provider.loadPlugins()
        assertEquals(1, plugins.size)
    }

    @Test
    fun testStalePluginsAreEvicted() {
        val registry = RemotePluginRegistry(
            keepaliveIntervalMs = 30_000,
            evictionThresholdMs = 500
        )

        val metadata = createTestMetadata("test-plugin", "http://localhost:8081")
            .copy(lastHeartbeat = Instant.now().minusSeconds(5))
        registry.register(metadata)

        Thread.sleep(600)
        registry.evictStale()

        val provider = RemotePayloadPluginProvider(registry, mockTransport)
        val plugins = provider.loadPlugins()

        assertEquals(0, plugins.size)
    }

    @Test
    fun testBuildPayloadThroughRemotePlugin() {
        val metadata = createTestMetadata("test-plugin", "http://localhost:8081")
        registry.register(metadata)

        val plugins = provider.loadPlugins()
        val plugin = plugins[0]

        val request = PayloadBuildRequest(
            workerId = "worker1",
            nowMillis = System.currentTimeMillis(),
            taskTtlMillis = 60_000,
            requestedPluginId = "test-plugin"
        )

        val task = plugin.buildPayload(request)

        assertNotNull(task)
        assertEquals("test-plugin", task.pluginId)
    }

    @Test
    fun testValidateResultThroughRemotePlugin() {
        val metadata = createTestMetadata("test-plugin", "http://localhost:8081")
        registry.register(metadata)

        val plugins = provider.loadPlugins()
        val plugin = plugins[0]

        val task = Task(
            jobId = UUID.randomUUID(),
            pluginId = "test-plugin",
            expiresAt = System.currentTimeMillis() + 60_000,
            payload = mockTransport.createTestPayload()
        )

        val result = ResultMessage(
            jobId = task.jobId,
            pluginId = "test-plugin",
            payload = mockTransport.createTestPayload(),
            durationMs = 1L,
            attempts = 1L
        )

        val validation = plugin.validateResult(task, result)

        assertNotNull(validation)
    }

    @Test
    fun testSupportsContextCheck() {
        val metadata = createTestMetadata("test-plugin", "http://localhost:8081")
        registry.register(metadata)

        val plugins = provider.loadPlugins()
        val plugin = plugins[0]

        val context = PayloadSupportContext(
            requestedPluginId = "test-plugin",
            workerId = "worker1"
        )

        val supports = plugin.supports(context)
        assertTrue(supports)
    }

    @Test
    fun testDisabledPluginMarkingUnhealthy() {
        registry.register(createTestMetadata("test-plugin", "http://localhost:8081"))

        provider.disable("test-plugin")

        val plugins = provider.loadPlugins()
        assertEquals(0, plugins.size)
    }

    @Test
    fun testCircuitBreakerTriggersOnFailure() {
        val registry = RemotePluginRegistry()
        val circuitBreaker = CircuitBreakerPluginTransport(
            RestPluginTransport(),
            registry,
            io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.ofDefaults()
        )

        val metadata = createTestMetadata("failing-plugin", "http://localhost:8081")
        registry.register(metadata)

        repeat(3) {
            try {
                kotlinx.coroutines.runBlocking {
                    circuitBreaker.buildPayload(
                        metadata,
                        PayloadBuildRequest("worker", System.currentTimeMillis(), 60_000)
                    )
                }
            } catch (e: Exception) {
                // Expected
            }
        }

        val plugins = provider.loadPlugins()
        assertEquals(0, plugins.size)
    }

    private fun createTestMetadata(id: String, baseUrl: String): PluginMetadata {
        return PluginMetadata(
            id = id,
            version = "1.0.0",
            contractVersion = "0.1.8",
            baseUrl = baseUrl,
            lastHeartbeat = Instant.now(),
            status = PluginStatus.HEALTHY,
            registeredAt = Instant.now()
        )
    }
}

class MockPluginTransport : PluginTransport {
    override suspend fun buildPayload(plugin: PluginMetadata, request: PayloadBuildRequest): Task {
        return Task(
            jobId = UUID.randomUUID(),
            pluginId = plugin.id,
            expiresAt = request.nowMillis + request.taskTtlMillis,
            payload = createTestPayload()
        )
    }

    override suspend fun validateResult(plugin: PluginMetadata, task: Task, result: ResultMessage): ValidationResult {
        return ValidationResult(ValidationStatus.ACCEPTED)
    }

    override suspend fun supports(plugin: PluginMetadata, context: PayloadSupportContext): Boolean {
        return true
    }

    override suspend fun health(plugin: PluginMetadata): Boolean {
        return true
    }

    fun createTestPayload() = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode()
}

class FailingPluginTransport : PluginTransport {
    override suspend fun buildPayload(plugin: PluginMetadata, request: PayloadBuildRequest): Task {
        throw RuntimeException("Simulated failure")
    }

    override suspend fun validateResult(plugin: PluginMetadata, task: Task, result: ResultMessage): ValidationResult {
        throw RuntimeException("Simulated failure")
    }

    override suspend fun supports(plugin: PluginMetadata, context: PayloadSupportContext): Boolean {
        throw RuntimeException("Simulated failure")
    }

    override suspend fun health(plugin: PluginMetadata): Boolean {
        throw RuntimeException("Simulated failure")
    }
}

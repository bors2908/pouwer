package ge.becrin.pouwer.pow.service

import tools.jackson.databind.node.JsonNodeFactory
import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginStatus
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.UUID

class CircuitBreakerPluginTransportTest {

    @Test
    fun testBuildPayloadReturnsDelegateResult() = runBlocking {
        val plugin = testMetadata()
        val registry = RemotePluginRegistry().also { it.register(plugin) }
        val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        val expectedTask = Task(
            jobId = UUID.randomUUID(),
            pluginId = plugin.id,
            expiresAt = System.currentTimeMillis() + 60_000,
            payload = JsonNodeFactory.instance.objectNode()
        )
        val restTemplate = StubRestTemplate(
            postResponses = mapOf("${plugin.baseUrl}/plugin/payload/build" to expectedTask)
        )
        val transport = CircuitBreakerPluginTransport(RestPluginTransport(restTemplate), registry, circuitBreakerRegistry)

        val result = transport.buildPayload(
            plugin,
            PayloadBuildRequest(workerId = "worker-1", nowMillis = System.currentTimeMillis(), taskTtlMillis = 60_000)
        )

        assertEquals(expectedTask, result)
    }

    @Test
    fun testValidateResultReturnsDelegateResult() = runBlocking {
        val plugin = testMetadata()
        val registry = RemotePluginRegistry().also { it.register(plugin) }
        val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        val expected = ValidationResult(ValidationStatus.ACCEPTED)
        val restTemplate = StubRestTemplate(
            postResponses = mapOf("${plugin.baseUrl}/plugin/payload/validate" to expected)
        )
        val transport = CircuitBreakerPluginTransport(RestPluginTransport(restTemplate), registry, circuitBreakerRegistry)

        val result = transport.validateResult(plugin, createTask(plugin.id), createResult(plugin.id))

        assertEquals(expected, result)
    }

    @Test
    fun testBuildPayloadMarksUnhealthyWhenDelegateThrows() {
        val plugin = testMetadata()
        val registry = RemotePluginRegistry().also { it.register(plugin) }
        val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        val transport = CircuitBreakerPluginTransport(
            RestPluginTransport(StubRestTemplate(response = RuntimeException("boom"))),
            registry,
            circuitBreakerRegistry
        )

        assertThrows(PluginTransportException::class.java) {
            runBlocking {
                transport.buildPayload(
                    plugin,
                    PayloadBuildRequest(workerId = "worker-1", nowMillis = System.currentTimeMillis(), taskTtlMillis = 60_000)
                )
            }
        }

        assertEquals(PluginStatus.UNHEALTHY, registry.get(plugin.id)?.status)
    }

    @Test
    fun testBuildPayloadMarksUnhealthyWhenCircuitOpen() {
        val plugin = testMetadata()
        val registry = RemotePluginRegistry().also { it.register(plugin) }
        val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("pluginTransport")
        circuitBreaker.transitionToOpenState()
        val transport = CircuitBreakerPluginTransport(RestPluginTransport(StubRestTemplate()), registry, circuitBreakerRegistry)

        assertThrows(PluginTransportException::class.java) {
            runBlocking {
                transport.buildPayload(
                    plugin,
                    PayloadBuildRequest(workerId = "worker-1", nowMillis = System.currentTimeMillis(), taskTtlMillis = 60_000)
                )
            }
        }

        assertEquals(PluginStatus.UNHEALTHY, registry.get(plugin.id)?.status)
    }

    @Test
    fun testValidateResultMarksUnhealthyWhenCircuitOpen() {
        val plugin = testMetadata()
        val registry = RemotePluginRegistry().also { it.register(plugin) }
        val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        circuitBreakerRegistry.circuitBreaker("pluginTransport").transitionToOpenState()
        val transport = CircuitBreakerPluginTransport(RestPluginTransport(StubRestTemplate()), registry, circuitBreakerRegistry)

        assertThrows(PluginTransportException::class.java) {
            runBlocking {
                transport.validateResult(plugin, createTask(plugin.id), createResult(plugin.id))
            }
        }

        assertEquals(PluginStatus.UNHEALTHY, registry.get(plugin.id)?.status)
    }

    @Test
    fun testSupportsReturnsFalseWhenCircuitOpen() = runBlocking {
        val plugin = testMetadata()
        val registry = RemotePluginRegistry().also { it.register(plugin) }
        val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        circuitBreakerRegistry.circuitBreaker("pluginTransport").transitionToOpenState()
        val transport = CircuitBreakerPluginTransport(RestPluginTransport(StubRestTemplate()), registry, circuitBreakerRegistry)

        val result = transport.supports(plugin, PayloadSupportContext(requestedPluginId = plugin.id))

        assertFalse(result)
        assertEquals(PluginStatus.UNHEALTHY, registry.get(plugin.id)?.status)
    }

    @Test
    fun testHealthReturnsFalseWhenCircuitOpen() = runBlocking {
        val plugin = testMetadata()
        val registry = RemotePluginRegistry().also { it.register(plugin) }
        val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        circuitBreakerRegistry.circuitBreaker("pluginTransport").transitionToOpenState()
        val transport = CircuitBreakerPluginTransport(RestPluginTransport(StubRestTemplate()), registry, circuitBreakerRegistry)

        val result = transport.health(plugin)

        assertFalse(result)
        assertEquals(PluginStatus.UNHEALTHY, registry.get(plugin.id)?.status)
    }

    private fun testMetadata(): PluginMetadata = PluginMetadata(
        id = "plugin-1",
        version = "1.0.0",
        contractVersion = CHALLENGE_PLUGIN_CONTRACT_VERSION,
        baseUrl = "http://localhost:8081",
        lastHeartbeat = Instant.now(),
        status = PluginStatus.HEALTHY
    )

    private fun createTask(pluginId: String): Task = Task(
        jobId = UUID.randomUUID(),
        pluginId = pluginId,
        expiresAt = System.currentTimeMillis() + 60_000,
        payload = JsonNodeFactory.instance.objectNode()
    )

    private fun createResult(pluginId: String): ResultMessage = ResultMessage(
        jobId = UUID.randomUUID(),
        pluginId = pluginId,
        payload = JsonNodeFactory.instance.objectNode(),
        durationMs = null,
        attempts = null
    )

    private class StubRestTemplate(
        private val response: Any? = null,
        private val postResponses: Map<String, Any?> = emptyMap()
    ) : RestTemplate() {
        override fun <T : Any> postForObject(url: String, request: Any?, responseType: Class<T>, vararg uriVariables: Any?): T? {
            if (response is RuntimeException) {
                throw response
            }
            if (postResponses.containsKey(url)) {
                return responseType.cast(postResponses[url])
            }
            return responseType.cast(response)
        }

        override fun <T : Any> getForObject(url: String, responseType: Class<T>, vararg uriVariables: Any?): T? {
            return responseType.cast(null)
        }
    }
}

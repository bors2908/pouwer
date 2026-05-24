package ge.becrin.pouwer.pow.service

import tools.jackson.databind.node.JsonNodeFactory
import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.UUID

class RestPluginTransportTest {

    @Test
    fun testBuildPayloadReturnsDelegateResult() = runBlocking {
        val expectedTask = createTask()
        val transport = RestPluginTransport(StubRestTemplate(expectedTask))

        val result = transport.buildPayload(
            testMetadata(),
            PayloadBuildRequest(workerId = "worker-1", nowMillis = System.currentTimeMillis(), taskTtlMillis = 60_000)
        )

        assertEquals(expectedTask, result)
    }

    @Test
    fun testBuildPayloadThrowsOnException() {
        val transport = RestPluginTransport(StubRestTemplate(RuntimeException("boom")))

        assertThrows(PluginTransportException::class.java) {
            runBlocking {
                transport.buildPayload(
                    testMetadata(),
                    PayloadBuildRequest(workerId = "worker-1", nowMillis = System.currentTimeMillis(), taskTtlMillis = 60_000)
                )
            }
        }
    }

    @Test
    fun testBuildPayloadThrowsOnNullResponse() {
        val transport = RestPluginTransport(StubRestTemplate(null))

        assertThrows(PluginTransportException::class.java) {
            runBlocking {
                transport.buildPayload(
                    testMetadata(),
                    PayloadBuildRequest(workerId = "worker-1", nowMillis = System.currentTimeMillis(), taskTtlMillis = 60_000)
                )
            }
        }
    }

    @Test
    fun testValidateResultThrowsOnNullResponse() {
        val transport = RestPluginTransport(StubRestTemplate(null))

        assertThrows(PluginTransportException::class.java) {
            runBlocking {
                transport.validateResult(testMetadata(), createTask(), createResult())
            }
        }
    }

    @Test
    fun testValidateResultThrowsOnException() {
        val transport = RestPluginTransport(StubRestTemplate(RuntimeException("boom")))

        assertThrows(PluginTransportException::class.java) {
            runBlocking {
                transport.validateResult(testMetadata(), createTask(), createResult())
            }
        }
    }

    @Test
    fun testSupportsReturnsFalseOnNullResponse() = runBlocking {
        val transport = RestPluginTransport(StubRestTemplate(null))

        val result = transport.supports(testMetadata(), PayloadSupportContext(requestedPluginId = "plugin-1"))

        assertFalse(result)
    }

    @Test
    fun testSupportsReturnsFalseOnException() = runBlocking {
        val transport = RestPluginTransport(StubRestTemplate(RuntimeException("boom")))

        val result = transport.supports(testMetadata(), PayloadSupportContext(requestedPluginId = "plugin-1"))

        assertFalse(result)
    }

    @Test
    fun testHealthReturnsFalseOnNullResponse() = runBlocking {
        val transport = RestPluginTransport(StubRestTemplate(null))

        val result = transport.health(testMetadata())

        assertFalse(result)
    }

    @Test
    fun testHealthReturnsTrueOnResponse() = runBlocking {
        val transport = RestPluginTransport(StubRestTemplate("ok"))

        val result = transport.health(testMetadata())

        assertTrue(result)
    }

    @Test
    fun testHealthReturnsFalseOnException() = runBlocking {
        val transport = RestPluginTransport(StubRestTemplate(RuntimeException("boom")))

        val result = transport.health(testMetadata())

        assertFalse(result)
    }

    private fun testMetadata(): PluginMetadata = PluginMetadata(
        id = "plugin-1",
        version = "1.0.0",
        contractVersion = CHALLENGE_PLUGIN_CONTRACT_VERSION,
        baseUrl = "http://localhost:8081",
        lastHeartbeat = Instant.now()
    )

    private fun createTask(): Task = Task(
        jobId = UUID.randomUUID(),
        pluginId = "plugin-1",
        expiresAt = System.currentTimeMillis() + 60_000,
        payload = JsonNodeFactory.instance.objectNode()
    )

    private fun createResult(): ResultMessage = ResultMessage(
        jobId = UUID.randomUUID(),
        pluginId = "plugin-1",
        payload = JsonNodeFactory.instance.objectNode(),
        durationMs = null,
        attempts = null
    )

    private class StubRestTemplate(
        private val response: Any?
    ) : RestTemplate() {
        override fun <T : Any> postForObject(url: String, request: Any?, responseType: Class<T>, vararg uriVariables: Any?): T? {
            if (response is RuntimeException) {
                throw response
            }
            return responseType.cast(response)
        }

        override fun <T : Any> getForObject(url: String, responseType: Class<T>, vararg uriVariables: Any?): T? {
            if (response is RuntimeException) {
                throw response
            }
            return responseType.cast(response)
        }
    }
}

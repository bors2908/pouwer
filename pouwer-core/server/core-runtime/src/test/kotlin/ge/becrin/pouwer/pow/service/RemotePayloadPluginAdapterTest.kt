package ge.becrin.pouwer.pow.service

import tools.jackson.databind.node.JsonNodeFactory
import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginTransport
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RemotePayloadPluginAdapterTest {

    @Test
    fun testSupportsReturnsTrueFromTransport() {
        val transport = TestTransport(supportsResult = true)
        val adapter = RemotePayloadPluginAdapter(testMetadata(), transport)

        val result = adapter.supports(PayloadSupportContext(requestedPluginId = "plugin-1"))

        assertEquals(true, result)
    }

    @Test
    fun testSupportsReturnsFalseOnTransportFailure() {
        val transport = TestTransport(supportsThrows = true)
        val adapter = RemotePayloadPluginAdapter(testMetadata(), transport)

        val result = adapter.supports(PayloadSupportContext(requestedPluginId = "plugin-1"))

        assertFalse(result)
    }

    @Test
    fun testBuildPayloadDelegatesToTransport() {
        val expectedTask = createTask()
        val transport = TestTransport(buildPayloadResult = expectedTask)
        val adapter = RemotePayloadPluginAdapter(testMetadata(), transport)

        val result = adapter.buildPayload(
            PayloadBuildRequest(
                workerId = "worker-1",
                nowMillis = System.currentTimeMillis(),
                taskTtlMillis = 60_000
            )
        )

        assertEquals(expectedTask, result)
    }

    @Test
    fun testBuildPayloadThrowsOnTransportFailure() {
        val transport = TestTransport(buildPayloadThrows = true)
        val adapter = RemotePayloadPluginAdapter(testMetadata(), transport)

        assertThrows(PluginTransportException::class.java) {
            adapter.buildPayload(
                PayloadBuildRequest(
                    workerId = "worker-1",
                    nowMillis = System.currentTimeMillis(),
                    taskTtlMillis = 60_000
                )
            )
        }
    }

    @Test
    fun testValidateResultDelegatesToTransport() {
        val expected = ValidationResult(ValidationStatus.ACCEPTED)
        val transport = TestTransport(validateResultResult = expected)
        val adapter = RemotePayloadPluginAdapter(testMetadata(), transport)

        val result = adapter.validateResult(createTask(), createResult())

        assertEquals(expected, result)
    }

    @Test
    fun testValidateResultThrowsOnTransportFailure() {
        val transport = TestTransport(validateResultThrows = true)
        val adapter = RemotePayloadPluginAdapter(testMetadata(), transport)

        assertThrows(PluginTransportException::class.java) {
            adapter.validateResult(createTask(), createResult())
        }
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

    private class TestTransport(
        private val supportsResult: Boolean = true,
        private val supportsThrows: Boolean = false,
        private val buildPayloadResult: Task? = null,
        private val buildPayloadThrows: Boolean = false,
        private val validateResultResult: ValidationResult? = null,
        private val validateResultThrows: Boolean = false
    ) : PluginTransport {
        override suspend fun buildPayload(plugin: PluginMetadata, request: PayloadBuildRequest): Task {
            if (buildPayloadThrows) {
                throw IllegalStateException("build failure")
            }
            return buildPayloadResult ?: Task(
                jobId = UUID.randomUUID(),
                pluginId = plugin.id,
                expiresAt = System.currentTimeMillis() + 60_000,
                payload = JsonNodeFactory.instance.objectNode()
            )
        }

        override suspend fun validateResult(
            plugin: PluginMetadata,
            task: Task,
            result: ResultMessage
        ): ValidationResult {
            if (validateResultThrows) {
                throw IllegalStateException("validate failure")
            }
            return validateResultResult ?: ValidationResult(ValidationStatus.ACCEPTED)
        }

        override suspend fun supports(plugin: PluginMetadata, context: PayloadSupportContext): Boolean {
            if (supportsThrows) {
                throw IllegalStateException("supports failure")
            }
            return supportsResult
        }

        override suspend fun health(plugin: PluginMetadata): Boolean = true
    }
}

package ge.becrin.pouwer.pow.service

import tools.jackson.databind.node.JsonNodeFactory
import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import ge.becrin.pouwer.pow.service.store.InMemoryTaskStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class ValidationPipelineTest {

    @Test
    fun testFailOnPluginMismatch() {
        val plugin = TestPlugin("pow-test-sha256", ValidationResult(ValidationStatus.ACCEPTED))
        val taskStore = InMemoryTaskStore().also {
            it.save(
                Task(
                    jobId = JOB_ID,
                    pluginId = plugin.id(),
                    expiresAt = System.currentTimeMillis() + 60_000,
                    payload = JsonNodeFactory.instance.objectNode()
                )
            )
        }

        val pipeline = ValidationPipeline(
            taskStore = taskStore,
            payloadPluginRegistry = PayloadPluginRegistry(listOf(plugin))
        )

        val result = pipeline.validate(
            ResultMessage(
                jobId = JOB_ID,
                pluginId = "other-plugin",
                payload = JsonNodeFactory.instance.objectNode(),
                durationMs = null,
                attempts = null
            )
        )

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    @Test
    fun testReturnConflictOnUnknownTask() {
        val pipeline = ValidationPipeline(
            taskStore = InMemoryTaskStore(),
            payloadPluginRegistry = PayloadPluginRegistry(emptyList())
        )

        val result = pipeline.validate(
            ResultMessage(
                jobId = UUID.randomUUID(),
                pluginId = "pow-test-sha256",
                payload = JsonNodeFactory.instance.objectNode(),
                durationMs = null,
                attempts = null
            )
        )

        assertEquals(ValidationStatus.CONFLICT, result.status)
    }

    @Test
    fun testRejectUnsupportedPlugin() {
        val taskStore = InMemoryTaskStore().also {
            it.save(
                Task(
                    jobId = JOB_ID,
                    pluginId = "pow-test-sha256",
                    expiresAt = System.currentTimeMillis() + 60_000,
                    payload = JsonNodeFactory.instance.objectNode()
                )
            )
        }

        val pipeline = ValidationPipeline(
            taskStore = taskStore,
            payloadPluginRegistry = PayloadPluginRegistry(emptyList())
        )

        val result = pipeline.validate(
            ResultMessage(
                jobId = JOB_ID,
                pluginId = null,
                payload = JsonNodeFactory.instance.objectNode(),
                durationMs = null,
                attempts = null
            )
        )

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    @Test
    fun testAcceptsValidResult() {
        val plugin = TestPlugin("pow-test-sha256", ValidationResult(ValidationStatus.ACCEPTED))
        val taskStore = InMemoryTaskStore().also {
            it.save(
                Task(
                    jobId = JOB_ID,
                    pluginId = plugin.id(),
                    expiresAt = System.currentTimeMillis() + 60_000,
                    payload = JsonNodeFactory.instance.objectNode()
                )
            )
        }
        val pipeline = ValidationPipeline(
            taskStore = taskStore,
            payloadPluginRegistry = PayloadPluginRegistry(listOf(plugin))
        )

        val result = pipeline.validate(
            ResultMessage(
                jobId = JOB_ID,
                pluginId = plugin.id(),
                payload = JsonNodeFactory.instance.objectNode(),
                durationMs = null,
                attempts = null
            )
        )

        assertEquals(ValidationStatus.ACCEPTED, result.status)
    }

    @Test
    fun testUsesTaskPluginWhenResultPluginIdMissing() {
        val plugin = TestPlugin("pow-test-sha256", ValidationResult(ValidationStatus.ACCEPTED))
        val taskStore = InMemoryTaskStore().also {
            it.save(
                Task(
                    jobId = JOB_ID,
                    pluginId = plugin.id(),
                    expiresAt = System.currentTimeMillis() + 60_000,
                    payload = JsonNodeFactory.instance.objectNode()
                )
            )
        }
        val pipeline = ValidationPipeline(
            taskStore = taskStore,
            payloadPluginRegistry = PayloadPluginRegistry(listOf(plugin))
        )

        val result = pipeline.validate(
            ResultMessage(
                jobId = JOB_ID,
                pluginId = null,
                payload = JsonNodeFactory.instance.objectNode(),
                durationMs = null,
                attempts = null
            )
        )

        assertEquals(ValidationStatus.ACCEPTED, result.status)
    }

    @Test
    fun testReturnsConflictOnReplayAfterAcceptedValidation() {
        val plugin = TestPlugin("pow-test-sha256", ValidationResult(ValidationStatus.ACCEPTED))
        val taskStore = InMemoryTaskStore().also {
            it.save(
                Task(
                    jobId = JOB_ID,
                    pluginId = plugin.id(),
                    expiresAt = System.currentTimeMillis() + 60_000,
                    payload = JsonNodeFactory.instance.objectNode()
                )
            )
        }
        val pipeline = ValidationPipeline(
            taskStore = taskStore,
            payloadPluginRegistry = PayloadPluginRegistry(listOf(plugin))
        )
        val resultMessage = ResultMessage(
            jobId = JOB_ID,
            pluginId = plugin.id(),
            payload = JsonNodeFactory.instance.objectNode(),
            durationMs = null,
            attempts = null
        )

        val first = pipeline.validate(resultMessage)
        val second = pipeline.validate(resultMessage)

        assertEquals(ValidationStatus.ACCEPTED, first.status)
        assertEquals(ValidationStatus.CONFLICT, second.status)
    }

    @Test
    fun testRejectsExpiredTask() {
        val plugin = TestPlugin("pow-test-sha256", ValidationResult(ValidationStatus.ACCEPTED))
        val taskStore = InMemoryTaskStore().also {
            it.save(
                Task(
                    jobId = JOB_ID,
                    pluginId = plugin.id(),
                    expiresAt = 1000L,
                    payload = JsonNodeFactory.instance.objectNode()
                )
            )
        }
        val pipeline = ValidationPipeline(
            taskStore = taskStore,
            payloadPluginRegistry = PayloadPluginRegistry(listOf(plugin)),
            currentTimeMillis = { 2000L }
        )

        val result = pipeline.validate(
            ResultMessage(
                jobId = JOB_ID,
                pluginId = plugin.id(),
                payload = JsonNodeFactory.instance.objectNode(),
                durationMs = null,
                attempts = null
            )
        )

        assertEquals(ValidationStatus.REJECTED, result.status)
        assertEquals("Task expired", result.reason)
    }

    @Test
    fun testRejectsWhenPluginThrows() {
        val plugin = TestPlugin(
            pluginId = "pow-test-sha256",
            response = ValidationResult(ValidationStatus.ACCEPTED),
            throwOnValidate = true
        )
        val taskStore = InMemoryTaskStore().also {
            it.save(
                Task(
                    jobId = JOB_ID,
                    pluginId = plugin.id(),
                    expiresAt = System.currentTimeMillis() + 60_000,
                    payload = JsonNodeFactory.instance.objectNode()
                )
            )
        }
        val pipeline = ValidationPipeline(
            taskStore = taskStore,
            payloadPluginRegistry = PayloadPluginRegistry(listOf(plugin))
        )

        val result = pipeline.validate(
            ResultMessage(
                jobId = JOB_ID,
                pluginId = plugin.id(),
                payload = JsonNodeFactory.instance.objectNode(),
                durationMs = null,
                attempts = null
            )
        )

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    private data class TestPlugin(
        private val pluginId: String,
        private val response: ValidationResult,
        private val throwOnValidate: Boolean = false
    ) : PayloadPlugin {
        override val contractVersion: String = CHALLENGE_PLUGIN_CONTRACT_VERSION

        override fun id(): String = pluginId

        override fun version(): String = "test"

        override fun supports(context: PayloadSupportContext): Boolean = true

        override fun buildPayload(request: PayloadBuildRequest): Task {
            throw UnsupportedOperationException("Not required for this test")
        }

        override fun validateResult(task: Task, result: ResultMessage): ValidationResult {
            check(!throwOnValidate) { "boom" }
            return response
        }
    }

    companion object {
        private val JOB_ID: UUID = UUID.randomUUID()
    }
}

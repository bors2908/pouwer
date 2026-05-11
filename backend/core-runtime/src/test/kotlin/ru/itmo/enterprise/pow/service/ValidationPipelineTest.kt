package ru.itmo.enterprise.pow.service

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.itmo.enterprise.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ru.itmo.enterprise.challenge.api.PayloadPlugin
import ru.itmo.enterprise.challenge.api.ResultMessage
import ru.itmo.enterprise.challenge.api.Task
import ru.itmo.enterprise.challenge.api.ValidationResult
import ru.itmo.enterprise.challenge.api.ValidationStatus
import ru.itmo.enterprise.pow.service.store.InMemoryTaskStore
import java.util.UUID

class ValidationPipelineTest {

    @Test
    fun `should use task plugin when result plugin id is missing`() {
        val plugin = TestPlugin("pow-test-sha256", ValidationResult(ValidationStatus.ACCEPTED))
        val taskStore = InMemoryTaskStore().also {
            it.save(
                Task(
                    jobId = JOB_ID,
                    pluginId = plugin.pluginId,
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
    fun `should return conflict for unknown task`() {
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
    fun `should reject unsupported plugin`() {
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

    private data class TestPlugin(
        override val pluginId: String,
        private val response: ValidationResult
    ) : PayloadPlugin {
        override val contractVersion: String = CHALLENGE_PLUGIN_CONTRACT_VERSION

        override fun createTask(workerId: String?, nowMillis: Long, taskTtlMillis: Long): Task {
            throw UnsupportedOperationException("Not required for this test")
        }

        override fun validate(task: Task, result: ResultMessage): ValidationResult = response
    }

    companion object {
        private val JOB_ID: UUID = UUID.randomUUID()
    }
}

package ge.becrin.pouwer.plugin.base

import tools.jackson.databind.node.JsonNodeFactory
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class PluginPayloadServiceTest {

    @Test
    fun testSupportsAllowsNullRequest() {
        val service = TestService("plugin-1")

        val result = service.supports(PayloadSupportContext(requestedPluginId = null))

        assertTrue(result)
    }

    @Test
    fun testSupportsMatchesRequestedId() {
        val service = TestService("plugin-1")

        val result = service.supports(PayloadSupportContext(requestedPluginId = "plugin-1"))

        assertTrue(result)
    }

    @Test
    fun testSupportsRejectsDifferentId() {
        val service = TestService("plugin-1")

        val result = service.supports(PayloadSupportContext(requestedPluginId = "plugin-2"))

        assertFalse(result)
    }

    @Test
    fun testBuildPayloadDelegatesToCreateTask() {
        val service = TestService("plugin-1")
        val request = PayloadBuildRequest(
            workerId = "worker-1",
            nowMillis = 1000,
            taskTtlMillis = 5000,
            requestedPluginId = "plugin-1"
        )

        val task = service.buildPayload(request)

        assertEquals(request.workerId, service.lastWorkerId)
        assertEquals(request.nowMillis, service.lastNowMillis)
        assertEquals(request.taskTtlMillis, service.lastTtlMillis)
        assertEquals(task, service.lastTask)
    }

    private class TestService(
        override val pluginId: String
    ) : PluginPayloadService {
        var lastWorkerId: String? = null
        var lastNowMillis: Long? = null
        var lastTtlMillis: Long? = null
        var lastTask: Task? = null

        override fun createTask(workerId: String?, nowMillis: Long, taskTtlMillis: Long): Task {
            lastWorkerId = workerId
            lastNowMillis = nowMillis
            lastTtlMillis = taskTtlMillis
            return Task(
                jobId = UUID.randomUUID(),
                pluginId = pluginId,
                expiresAt = nowMillis + taskTtlMillis,
                payload = JsonNodeFactory.instance.objectNode()
            ).also { lastTask = it }
        }

        override fun validate(task: Task, result: ResultMessage): ValidationResult =
            ValidationResult(ValidationStatus.ACCEPTED)
    }
}

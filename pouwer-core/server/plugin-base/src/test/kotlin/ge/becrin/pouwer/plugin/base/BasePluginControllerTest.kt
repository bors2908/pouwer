package ge.becrin.pouwer.plugin.base

import tools.jackson.databind.node.JsonNodeFactory
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PluginValidationRequest
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

class BasePluginControllerTest {

    @Test
    fun testBuildPayloadReturnsOk() {
        val task = createTask("plugin-1")
        val service = TestPayloadService(buildPayloadResult = task)
        val controller = TestController(service)

        val response = controller.buildPayload(
            PayloadBuildRequest(
                workerId = "worker-1",
                nowMillis = 1000,
                taskTtlMillis = 5000
            )
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(task, response.body)
    }

    @Test
    fun testBuildPayloadReturnsServerErrorOnException() {
        val service = TestPayloadService(buildPayloadThrows = true)
        val controller = TestController(service)

        val response = controller.buildPayload(
            PayloadBuildRequest(
                workerId = "worker-1",
                nowMillis = 1000,
                taskTtlMillis = 5000
            )
        )

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNull(response.body)
    }

    @Test
    fun testValidateReturnsOk() {
        val service = TestPayloadService(validateResult = ValidationResult(ValidationStatus.ACCEPTED))
        val controller = TestController(service)

        val response = controller.validateResult(
            PluginValidationRequest(
                task = createTask("plugin-1"),
                result = createResult("plugin-1")
            )
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(ValidationStatus.ACCEPTED, response.body?.status)
    }

    @Test
    fun testValidateReturnsServerErrorOnException() {
        val service = TestPayloadService(validateThrows = true)
        val controller = TestController(service)

        val response = controller.validateResult(
            PluginValidationRequest(
                task = createTask("plugin-1"),
                result = createResult("plugin-1")
            )
        )

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNull(response.body)
    }

    @Test
    fun testHealthReturnsUp() {
        val service = TestPayloadService()
        val controller = TestController(service)

        val response = controller.health()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("UP", response.body?.get("status"))
        assertEquals(service.pluginId, response.body?.get("pluginId"))
    }

    private class TestController(service: PluginPayloadService) : BasePluginController(service)

    private class TestPayloadService(
        private val buildPayloadResult: Task? = null,
        private val buildPayloadThrows: Boolean = false,
        private val validateResult: ValidationResult = ValidationResult(ValidationStatus.ACCEPTED),
        private val validateThrows: Boolean = false
    ) : PluginPayloadService {
        override val pluginId: String = "plugin-1"

        override fun createTask(workerId: String?, nowMillis: Long, taskTtlMillis: Long): Task {
            return buildPayloadResult ?: Task(
                jobId = UUID.randomUUID(),
                pluginId = pluginId,
                expiresAt = System.currentTimeMillis() + 60_000,
                payload = JsonNodeFactory.instance.objectNode()
            )
        }

        override fun validate(task: Task, result: ResultMessage): ValidationResult {
            if (validateThrows) {
                throw IllegalStateException("validate failed")
            }
            return validateResult
        }

        override fun buildPayload(request: PayloadBuildRequest): Task {
            if (buildPayloadThrows) {
                throw IllegalStateException("build failed")
            }
            return buildPayloadResult ?: Task(
                jobId = UUID.randomUUID(),
                pluginId = pluginId,
                expiresAt = System.currentTimeMillis() + 60_000,
                payload = JsonNodeFactory.instance.objectNode()
            )
        }
    }

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
}

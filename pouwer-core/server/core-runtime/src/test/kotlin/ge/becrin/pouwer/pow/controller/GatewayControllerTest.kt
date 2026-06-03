package ge.becrin.pouwer.pow.controller

import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.module.kotlin.jacksonObjectMapper
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import ge.becrin.pouwer.pow.service.PayloadPluginRegistry
import ge.becrin.pouwer.pow.service.ValidationPipeline
import ge.becrin.pouwer.pow.service.store.InMemoryTaskStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

class GatewayControllerTest {

    @Test
    fun testValidateAcceptedResponse() {
        val plugin = TestPlugin("plugin-ok")
        val taskStore = InMemoryTaskStore().also {
            it.save(createTask(plugin.id()))
        }
        val registry = PayloadPluginRegistry(listOf(plugin))
        val pipeline = ValidationPipeline(taskStore, registry)
        val controller = GatewayController(registry, taskStore, pipeline, jacksonObjectMapper())

        val response = controller.validate(
            ResultMessage(
                jobId = taskStore.find(JOB_ID)!!.jobId,
                pluginId = plugin.id(),
                payload = JsonNodeFactory.instance.objectNode(),
                durationMs = null,
                attempts = null
            )
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("accepted", body["status"])
    }

    @Test
    fun testValidateRejectedResponse() {
        val plugin = TestPlugin("plugin-ok")
        val taskStore = InMemoryTaskStore().also {
            it.save(createTask(plugin.id()))
        }
        val registry = PayloadPluginRegistry(listOf(plugin))
        val pipeline = ValidationPipeline(taskStore, registry)
        val controller = GatewayController(registry, taskStore, pipeline, jacksonObjectMapper())

        val response = controller.validate(
            ResultMessage(
                jobId = JOB_ID,
                pluginId = "other-plugin",
                payload = JsonNodeFactory.instance.objectNode(),
                durationMs = null,
                attempts = null
            )
        )

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("rejected", body["status"])
    }

    @Test
    fun testValidateConflictResponse() {
        val taskStore = InMemoryTaskStore()
        val registry = PayloadPluginRegistry(listOf(TestPlugin("plugin-ok")))
        val pipeline = ValidationPipeline(taskStore, registry)
        val controller = GatewayController(registry, taskStore, pipeline, jacksonObjectMapper())

        val response = controller.validate(
            ResultMessage(
                jobId = UUID.randomUUID(),
                pluginId = "plugin-ok",
                payload = JsonNodeFactory.instance.objectNode(),
                durationMs = null,
                attempts = null
            )
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("conflict", body["status"])
    }

    @Test
    fun testChallengeReturnsTaskAndStoresIt() {
        val task = createTask("plugin-ok")
        val plugin = TestPlugin("plugin-ok", buildPayloadResult = task)
        val registry = PayloadPluginRegistry(listOf(plugin))
        val taskStore = InMemoryTaskStore()
        val controller = GatewayController(registry, taskStore, ValidationPipeline(taskStore, registry), jacksonObjectMapper())

        val response = controller.challenge(workerId = "worker-1", pluginId = plugin.id())

        assertEquals(task.jobId, response.jobId)
        assertNotNull(taskStore.find(task.jobId))
    }

    @Test
    fun testChallengeThrowsWhenSupportsFalse() {
        val plugin = TestPlugin("plugin-ok")
        val registry = PayloadPluginRegistry(listOf(plugin))
        val taskStore = InMemoryTaskStore()
        val controller = GatewayController(registry, taskStore, ValidationPipeline(taskStore, registry), jacksonObjectMapper())

        assertThrows(IllegalArgumentException::class.java) {
            controller.challenge(workerId = "worker-1", pluginId = plugin.id())
        }
    }

    @Test
    fun testChallengeThrowsWhenSupportsFails() {
        val plugin = TestPlugin("plugin-ok")
        val registry = PayloadPluginRegistry(listOf(plugin))
        val taskStore = InMemoryTaskStore()
        val controller = GatewayController(registry, taskStore, ValidationPipeline(taskStore, registry), jacksonObjectMapper())

        val exception = assertThrows(ResponseStatusException::class.java) {
            controller.challenge(workerId = "worker-1", pluginId = plugin.id())
        }
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.statusCode)
    }

    @Test
    fun testChallengeThrowsWhenBuildPayloadFails() {
        val plugin = TestPlugin("plugin-ok", buildPayloadThrows = true)
        val registry = PayloadPluginRegistry(listOf(plugin))
        val taskStore = InMemoryTaskStore()
        val controller = GatewayController(registry, taskStore, ValidationPipeline(taskStore, registry), jacksonObjectMapper())

        val exception = assertThrows(ResponseStatusException::class.java) {
            controller.challenge(workerId = "worker-1", pluginId = plugin.id())
        }
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.statusCode)
    }

    @Test
    fun testChallengeWithoutPluginIdReturnsBadRequest() {
        val registry = PayloadPluginRegistry(listOf(TestPlugin("plugin-ok")))
        val taskStore = InMemoryTaskStore()
        val controller = GatewayController(registry, taskStore, ValidationPipeline(taskStore, registry), jacksonObjectMapper())

        val exception = assertThrows(ResponseStatusException::class.java) {
            controller.challenge(workerId = "worker-1", pluginId = null)
        }
        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
    }

    private fun createTask(pluginId: String): Task {
        return Task(
            jobId = JOB_ID,
            pluginId = pluginId,
            expiresAt = System.currentTimeMillis() + 60_000,
            payload = JsonNodeFactory.instance.objectNode()
        )
    }

    private class TestPlugin(
        private val pluginId: String,
        private val buildPayloadResult: Task? = null,
        private val buildPayloadThrows: Boolean = false
    ) : PayloadPlugin {
        override fun id(): String = pluginId

        override fun buildPayload(request: PayloadBuildRequest): Task {
            if (buildPayloadThrows) {
                throw IllegalStateException("Build failed")
            }
            return buildPayloadResult ?: Task(
                jobId = UUID.randomUUID(),
                pluginId = pluginId,
                expiresAt = System.currentTimeMillis() + 60_000,
                payload = JsonNodeFactory.instance.objectNode()
            )
        }

        override fun validateResult(task: Task, result: ResultMessage): ValidationResult =
            ValidationResult(ValidationStatus.ACCEPTED)
    }

    companion object {
        private val JOB_ID: UUID = UUID.randomUUID()
    }

}

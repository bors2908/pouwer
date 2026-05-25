package ge.becrin.pouwer.pow.controller

import tools.jackson.databind.ObjectMapper
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationStatus
import ge.becrin.pouwer.pow.service.PayloadPluginRegistry
import ge.becrin.pouwer.pow.service.TaskStore
import ge.becrin.pouwer.pow.service.ValidationPipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class GatewayController(
    private val payloadPluginRegistry: PayloadPluginRegistry,
    private val taskStore: TaskStore,
    private val validationPipeline: ValidationPipeline,
    private val objectMapper: ObjectMapper,
    @param:Value($$"${challenge.task.ttl-ms:60000}")
    private val taskTtlMillis: Long = 60_000,
) {
    @GetMapping("/challenge")
    fun challenge(
        @RequestParam(required = false) workerId: String?,
        @RequestParam(required = false) pluginId: String?
    ): Task {
        if (pluginId.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "pluginId is required")
        }
        val plugin = try {
            payloadPluginRegistry.get(pluginId)
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin not found: $pluginId")
        }
        val supportsRequest = try {
            plugin.supports(PayloadSupportContext(requestedPluginId = pluginId, workerId = workerId))
        } catch (e: Exception) {
            payloadPluginRegistry.disable(pluginId, e)
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Plugin $pluginId failed", e)
        }
        if (!supportsRequest) {
            throw IllegalArgumentException("Plugin $pluginId does not support this request")
        }
        val task = try {
            plugin.buildPayload(
                PayloadBuildRequest(
                    workerId = workerId,
                    nowMillis = System.currentTimeMillis(),
                    taskTtlMillis = taskTtlMillis,
                    requestedPluginId = pluginId
                )
            )
        } catch (e: Exception) {
            payloadPluginRegistry.disable(pluginId, e)
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Plugin $pluginId failed", e)
        }
        taskStore.save(task)
        log.info { "Task: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(task) }
        return task
    }

    @PostMapping("/validate")
    fun validate(@RequestBody result: ResultMessage): ResponseEntity<Any> {
        val validation = validationPipeline.validate(result)

        return when (validation.status) {
            ValidationStatus.ACCEPTED ->
                ResponseEntity.ok(mapOf("status" to "accepted"))

            ValidationStatus.REJECTED ->
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(mapOf("status" to "rejected", "reason" to validation.reason))

            ValidationStatus.CONFLICT ->
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(mapOf("status" to "conflict", "reason" to validation.reason))
        }
    }


    companion object {
        private val log = KotlinLogging.logger {}
    }
}

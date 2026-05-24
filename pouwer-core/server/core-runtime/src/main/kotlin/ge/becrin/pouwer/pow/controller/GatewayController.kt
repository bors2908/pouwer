package ge.becrin.pouwer.pow.controller

import tools.jackson.databind.ObjectMapper
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationStatus
import ge.becrin.pouwer.pow.service.PayloadPluginRegistry
import ge.becrin.pouwer.pow.service.RemotePluginRegistry
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
    private val remotePluginRegistry: RemotePluginRegistry? = null,
    @param:Value($$"${challenge.task.ttl-ms:60000}")
    private val taskTtlMillis: Long = 60_000,
    @param:Value($$"${challenge.plugins.priority-override:}")
    private val priorityOverrideRaw: String = ""
) {
    @GetMapping("/challenge")
    fun challenge(
        @RequestParam(required = false) workerId: String?,
        @RequestParam(required = false) pluginId: String?
    ): Task {
        val candidates = resolveCandidatePluginIds(pluginId)
        var lastError: Exception? = null

        for (candidatePluginId in candidates) {
            val plugin = try {
                payloadPluginRegistry.get(candidatePluginId)
            } catch (_: Exception) {
                continue
            }
            val supportsRequest = try {
                plugin.supports(
                    PayloadSupportContext(
                        requestedPluginId = pluginId,
                        workerId = workerId
                    )
                )
            } catch (e: Exception) {
                payloadPluginRegistry.disable(candidatePluginId, e)
                lastError = e
                continue
            }
            if (!supportsRequest) {
                continue
            }

            val task = try {
                plugin.buildPayload(
                    PayloadBuildRequest(
                        workerId = workerId,
                        nowMillis = System.currentTimeMillis(),
                        taskTtlMillis = taskTtlMillis,
                        requestedPluginId = pluginId ?: candidatePluginId
                    )
                )
            } catch (e: Exception) {
                payloadPluginRegistry.disable(candidatePluginId, e)
                lastError = e
                continue
            }

            taskStore.save(task)
            log.info { "Task: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(task) }
            return task
        }

        if (pluginId != null && lastError == null) {
            throw IllegalArgumentException("Plugin $pluginId does not support this request")
        }
        throw ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "No available plugin could build challenge",
            lastError
        )
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

    private fun resolveCandidatePluginIds(requestedPluginId: String?): List<String> {
        if (!requestedPluginId.isNullOrBlank()) {
            return listOf(requestedPluginId)
        }

        val available = payloadPluginRegistry.pluginIds()
        if (available.isEmpty()) {
            return emptyList()
        }

        val ordered = linkedSetOf<String>()
        val priorityOverride = priorityOverrideRaw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        priorityOverride
            .filter { available.contains(it) }
            .forEach { ordered.add(it) }

        remotePluginRegistry
            ?.getHealthy()
            ?.sortedByDescending { it.registeredAt }
            ?.map { it.id }
            ?.filter { available.contains(it) }
            ?.forEach { ordered.add(it) }

        available
            .filterNot { ordered.contains(it) }
            .forEach { ordered.add(it) }

        return ordered.toList()
    }
}

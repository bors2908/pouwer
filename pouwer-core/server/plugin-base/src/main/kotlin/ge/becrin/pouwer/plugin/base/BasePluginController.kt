package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PluginValidationRequest
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

abstract class BasePluginController(
    protected val payloadPlugin: PluginPayloadService
) {
    @PostMapping("/payload/build")
    fun buildPayload(@RequestBody request: PayloadBuildRequest): ResponseEntity<Task> {
        return try {
            ResponseEntity.ok(payloadPlugin.buildPayload(request))
        } catch (e: Exception) {
            log.error(e) { "Error building payload for ${payloadPlugin.pluginId}" }
            ResponseEntity.status(500).build()
        }
    }

    @PostMapping("/payload/validate")
    fun validateResult(@RequestBody payload: PluginValidationRequest): ResponseEntity<ValidationResult> {
        return try {
            ResponseEntity.ok(payloadPlugin.validate(payload.task, payload.result))
        } catch (e: Exception) {
            log.error(e) { "Error validating result for ${payloadPlugin.pluginId}" }
            ResponseEntity.status(500).build()
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "pluginId" to payloadPlugin.pluginId
            )
        )
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

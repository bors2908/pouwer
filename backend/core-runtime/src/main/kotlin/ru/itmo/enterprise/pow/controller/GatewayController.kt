package ru.itmo.enterprise.pow.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itmo.enterprise.challenge.api.ResultMessage
import ru.itmo.enterprise.challenge.api.Task
import ru.itmo.enterprise.challenge.api.ValidationStatus
import ru.itmo.enterprise.pow.config.PowProperties
import ru.itmo.enterprise.pow.service.LegacyJobTypeMapper
import ru.itmo.enterprise.pow.service.PayloadPluginRegistry
import ru.itmo.enterprise.pow.service.TaskStore
import ru.itmo.enterprise.pow.service.ValidationPipeline
import com.fasterxml.jackson.databind.ObjectMapper

@RestController
class GatewayController(
    private val payloadPluginRegistry: PayloadPluginRegistry,
    private val taskStore: TaskStore,
    private val validationPipeline: ValidationPipeline,
    private val powProperties: PowProperties,
    private val objectMapper: ObjectMapper
) {

    @GetMapping("/challenge")
    fun challenge(
        @RequestParam(required = false) workerId: String?,
        @RequestParam(required = false) jobType: String?,
        @RequestParam(required = false) pluginId: String?
    ): Task {
        val resolvedPluginId = LegacyJobTypeMapper.resolve(jobType = jobType, pluginId = pluginId)
        val plugin = payloadPluginRegistry.get(resolvedPluginId)

        val task = plugin.createTask(
            workerId = workerId,
            nowMillis = System.currentTimeMillis(),
            taskTtlMillis = powProperties.ttlSeconds * 1000
        )

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

    @PostMapping(
        path = [CAPTCHA_CUSTOM_VALIDATE_PATH],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun validateCustomCaptcha(
        @RequestParam(CAPTCHA_CUSTOM_RESPONSE) response: String
    ): ResponseEntity<Map<String, Boolean>> {
        val result = objectMapper.readValue(response, ResultMessage::class.java)
        val validation = validationPipeline.validate(result)

        return when (validation.status) {
            ValidationStatus.ACCEPTED ->
                ResponseEntity.ok(mapOf("success" to true))

            ValidationStatus.REJECTED, ValidationStatus.CONFLICT ->
                ResponseEntity.ok(mapOf("success" to false))
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}

        const val CAPTCHA_CUSTOM_VALIDATE_PATH: String = "/validate-custom-captcha"
        const val CAPTCHA_CUSTOM_RESPONSE: String = "response"
    }
}

package ru.itmo.enterprise.pow.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.ResultMessage
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.model.ValidationStatus
import ru.itmo.enterprise.pow.service.ResultService
import ru.itmo.enterprise.pow.service.TaskOrchestrator
import tools.jackson.databind.ObjectMapper

@RestController
class GatewayController(
    orchestrators: Collection<TaskOrchestrator>,
    resultServices: Collection<ResultService>,
    val objectMapper: ObjectMapper
) {
    private val orchestratorsByType = orchestrators.associateBy { it.type }
    private val resultServiceByType = resultServices.associateBy { it.type }

    @GetMapping("/challenge")
    fun challenge(
        @RequestParam(required = false) workerId: String?,
        //TODO Add default
        @RequestParam(required = false) jobType: JobType = JobType.POW_TEST_SHA256
    ): Task {
        val orchestrator = orchestratorsByType[jobType]
            ?: throw IllegalArgumentException("Unknown job type: $jobType")

        val task = orchestrator.createTask(workerId)

        log.info { "Task: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(task) }

        return task
    }

    @PostMapping("/validate")
    fun validate(@RequestBody result: ResultMessage): ResponseEntity<Any> {
        val resultService = resultServiceByType[result.jobType]
            ?: throw IllegalArgumentException("Unknown job type: ${result.jobType}")

        val validation = resultService.handleResult(result)

        return when (validation.status) {
            ValidationStatus.ACCEPTED ->
                ResponseEntity.ok(mapOf("status" to "accepted"))

            ValidationStatus.REJECTED ->
                ResponseEntity.unprocessableEntity()
                    .body(mapOf("status" to "rejected", "reason" to validation.reason))

            ValidationStatus.CONFLICT ->
                ResponseEntity.status(409)
                    .body(mapOf("status" to "conflict", "reason" to validation.reason))
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

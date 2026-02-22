package ru.itmo.enterprise.pow.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.itmo.enterprise.pow.model.ResultMessage
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.model.ValidationStatus
import ru.itmo.enterprise.pow.service.ResultService
import ru.itmo.enterprise.pow.service.TaskOrchestrator

@RestController
class GatewayController(
    private val orchestrator: TaskOrchestrator,
    private val resultService: ResultService
) {

    @GetMapping("/challenge")
    fun challenge(@RequestParam(required = false) workerId: String?): Task =
        orchestrator.createTask(workerId)

    @PostMapping("/validate")
    fun validate(@RequestBody result: ResultMessage): ResponseEntity<Any> {
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
}

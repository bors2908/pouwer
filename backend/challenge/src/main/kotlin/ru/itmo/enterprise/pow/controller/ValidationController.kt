package ru.itmo.enterprise.pow.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.itmo.enterprise.pow.model.ValidateRequest
import ru.itmo.enterprise.pow.service.NonceService
import ru.itmo.enterprise.pow.service.PowService

@RestController
class ValidationController(
    private val nonceService: NonceService,
    private val powService: PowService
) {
    @PostMapping("/validate")
    fun validate(@RequestBody request: ValidateRequest): ResponseEntity<Void> {
        val payload = nonceService.parseAndValidateNonce(request.nonce)
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
        }

        val isValid = powService.verify(request.nonce, request.solution, request.hash)
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
        }

        return ResponseEntity.ok().build()
    }
}

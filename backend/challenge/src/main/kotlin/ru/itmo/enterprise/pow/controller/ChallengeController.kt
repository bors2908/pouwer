package ru.itmo.enterprise.pow.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import ru.itmo.enterprise.pow.config.PowProperties
import ru.itmo.enterprise.pow.model.ChallengeResponse
import ru.itmo.enterprise.pow.service.NonceService

@RestController
class ChallengeController(
    private val nonceService: NonceService,
    private val props: PowProperties
) {
    @GetMapping("/challenge")
    fun getChallenge(): ChallengeResponse {
        val nonce = nonceService.generateNonce()
        return ChallengeResponse(
            nonce = nonce,
            difficulty = props.difficultyBits
        )
    }
}

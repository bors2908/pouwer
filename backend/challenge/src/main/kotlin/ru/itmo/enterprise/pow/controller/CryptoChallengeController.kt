package ru.itmo.enterprise.pow.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itmo.enterprise.pow.model.ChallengeResponseCrypto
import ru.itmo.enterprise.pow.model.ValidateRequestCrypto
import ru.itmo.enterprise.pow.service.CryptoChallengeService

@RestController
class CryptoChallengeController(
    private val cryptoService: CryptoChallengeService
) {
    @GetMapping("/challenge-crypto")
    fun getChallenge(@RequestParam(defaultValue = "0") worker: Int): ChallengeResponseCrypto {
        return cryptoService.getChallenge(worker)
    }

    @PostMapping("/validate-crypto")
    fun validate(@RequestBody request: ValidateRequestCrypto): Map<String, Any> {
        val success = cryptoService.validate(request)
        return if (success) {
            mapOf("status" to "ok")
        } else {
            mapOf("status" to "error")
        }
    }
}

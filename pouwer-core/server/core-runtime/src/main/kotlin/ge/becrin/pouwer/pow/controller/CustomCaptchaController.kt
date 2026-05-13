package ge.becrin.pouwer.pow.controller

import com.fasterxml.jackson.databind.ObjectMapper
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.ValidationStatus
import ge.becrin.pouwer.pow.service.ValidationPipeline
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CustomCaptchaController(
    private val validationPipeline: ValidationPipeline,
    private val objectMapper: ObjectMapper
) {
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
        const val CAPTCHA_CUSTOM_VALIDATE_PATH: String = "/validate-custom-captcha"
        const val CAPTCHA_CUSTOM_RESPONSE: String = "response"
    }
}

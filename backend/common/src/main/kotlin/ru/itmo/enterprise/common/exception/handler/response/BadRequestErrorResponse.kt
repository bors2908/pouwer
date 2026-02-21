package ru.itmo.enterprise.common.exception.handler.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Bad Request Error Response")
data class BadRequestErrorResponse(
    @param:Schema(description = "Description of the error", example = "Invalid input provided")
    override var message: String? = null,
) : ru.itmo.enterprise.common.exception.handler.response.ErrorResponse {
    @Schema(
        description = "HTTP status code of the error",
        example = ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse.Companion.CODE
    )
    override val code: String = ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse.Companion.CODE

    companion object {
        @JsonIgnore
        const val CODE: String = "400"
    }
}

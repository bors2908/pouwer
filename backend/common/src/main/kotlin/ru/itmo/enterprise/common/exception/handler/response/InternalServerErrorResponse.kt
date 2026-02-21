package ru.itmo.enterprise.common.exception.handler.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Internal Server Error Response")
data class InternalServerErrorResponse(
    @param:Schema(description = "Description of the error", example = "Internal Server Error")
    override var message: String? = null
) : ru.itmo.enterprise.common.exception.handler.response.ErrorResponse {
    @Schema(
        description = "HTTP status code of the error",
        example = ru.itmo.enterprise.common.exception.handler.response.InternalServerErrorResponse.Companion.CODE
    )
    override val code: String = ru.itmo.enterprise.common.exception.handler.response.InternalServerErrorResponse.Companion.CODE

    companion object {
        @JsonIgnore
        const val CODE: String = "500"
    }
}

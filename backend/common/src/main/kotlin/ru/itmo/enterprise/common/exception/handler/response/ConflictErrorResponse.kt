package ru.itmo.enterprise.common.exception.handler.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Conflict Error Response")
data class ConflictErrorResponse(
    @param:Schema(description = "Description of the error", example = "Data Conflict")
    override var message: String? = null
) : ru.itmo.enterprise.common.exception.handler.response.ErrorResponse {
    @Schema(
        description = "HTTP status code of the error",
        example = ru.itmo.enterprise.common.exception.handler.response.ConflictErrorResponse.Companion.CODE
    )
    override val code: String = ru.itmo.enterprise.common.exception.handler.response.ConflictErrorResponse.Companion.CODE

    companion object {
        @JsonIgnore
        const val CODE: String = "409"
    }
}

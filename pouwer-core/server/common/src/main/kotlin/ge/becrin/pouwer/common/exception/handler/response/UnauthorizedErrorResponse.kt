package ge.becrin.pouwer.common.exception.handler.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Error response object")
data class UnauthorizedErrorResponse(
    @param:Schema(description = "Description of the error", example = "Unauthorized")
    override var message: String? = null
) : ErrorResponse {
    @Schema(
        description = "HTTP status code of the error",
        example = CODE
    )
    override val code: String = CODE

    companion object {
        @JsonIgnore
        const val CODE: String = "401"
    }
}

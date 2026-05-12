package ge.becrin.pouwer.common.exception.handler.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Not Found Error Response")
data class NotFoundErrorResponse(
    @param:Schema(description = "Description of the error", example = "Not Found")
    override var message: String? = null
) : ge.becrin.pouwer.common.exception.handler.response.ErrorResponse {
    @Schema(
        description = "HTTP status code of the error",
        example = ge.becrin.pouwer.common.exception.handler.response.NotFoundErrorResponse.Companion.CODE
    )
    override val code: String = ge.becrin.pouwer.common.exception.handler.response.NotFoundErrorResponse.Companion.CODE

    companion object {
        @JsonIgnore
        const val CODE: String = "404"
    }
}

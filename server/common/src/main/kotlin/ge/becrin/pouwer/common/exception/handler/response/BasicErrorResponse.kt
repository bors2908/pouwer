package ge.becrin.pouwer.common.exception.handler.response

data class BasicErrorResponse(
    override val code: String?,
    override val message: String?
) : ge.becrin.pouwer.common.exception.handler.response.ErrorResponse

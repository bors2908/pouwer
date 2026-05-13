package ge.becrin.pouwer.common.exception

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import ge.becrin.pouwer.common.exception.handler.response.BadRequestErrorResponse
import ge.becrin.pouwer.common.exception.handler.response.ConflictErrorResponse
import ge.becrin.pouwer.common.exception.handler.response.ForbiddenErrorResponse
import ge.becrin.pouwer.common.exception.handler.response.InternalServerErrorResponse
import ge.becrin.pouwer.common.exception.handler.response.NotFoundErrorResponse
import ge.becrin.pouwer.common.exception.handler.response.UnauthorizedErrorResponse
import java.nio.file.AccessDeniedException

@ControllerAdvice
class ErrorHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponses(
        ApiResponse(
            responseCode = BadRequestErrorResponse.CODE,
            description = "Bad Request - Invalid argument",
            content = arrayOf(Content(schema = Schema(implementation = BadRequestErrorResponse::class)))
        )
    )
    fun handleIllegalArgumentException(exception: IllegalArgumentException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BadRequestErrorResponse(exception.message))
    }

    @ExceptionHandler(InsufficientPermissionsException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ApiResponses(
        ApiResponse(
            responseCode = ForbiddenErrorResponse.CODE,
            description = "Forbidden - Access Denied",
            content = arrayOf(Content(schema = Schema(implementation = ForbiddenErrorResponse::class)))
        )
    )
    fun handleInsufficientPermissionsException(exception: InsufficientPermissionsException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ForbiddenErrorResponse(exception.message))
    }

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ApiResponses(
        ApiResponse(
            responseCode = UnauthorizedErrorResponse.CODE,
            description = "Unauthorized - Access Denied",
            content = arrayOf(Content(schema = Schema(implementation = UnauthorizedErrorResponse::class)))
        )
    )
    fun handleAccessDeniedException(exception: AccessDeniedException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(UnauthorizedErrorResponse(exception.message))
    }

    @ExceptionHandler(DataNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ApiResponses(
        ApiResponse(
            responseCode = NotFoundErrorResponse.CODE,
            description = "Not Found - Data not found",
            content = arrayOf(Content(schema = Schema(implementation = NotFoundErrorResponse::class)))
        )
    )
    fun handleNotFoundException(exception: DataNotFoundException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(NotFoundErrorResponse(exception.message))
    }

    @ExceptionHandler(DataConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ApiResponses(
        ApiResponse(
            responseCode = ConflictErrorResponse.CODE,
            description = "Conflict - Data conflict",
            content = arrayOf(Content(schema = Schema(implementation = ConflictErrorResponse::class)))
        )
    )
    fun handleDataConflictException(exception: DataConflictException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ConflictErrorResponse(exception.message))
    }

    @ExceptionHandler(InvalidRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponses(
        ApiResponse(
            responseCode = BadRequestErrorResponse.CODE,
            description = "Bad Request - Invalid argument",
            content = arrayOf(Content(schema = Schema(implementation = BadRequestErrorResponse::class)))
        )
    )
    fun handleInvalidRequestException(exception: InvalidRequestException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(BadRequestErrorResponse(exception.message))
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ApiResponses(
        ApiResponse(
            responseCode = InternalServerErrorResponse.CODE,
            description = "Internal Server Error - Unknown error occurred",
            content = arrayOf(Content(schema = Schema(implementation = InternalServerErrorResponse::class)))
        )
    )
    fun handleAllUncaughtException(exception: Exception): ResponseEntity<Any> {
        logger.error("Unknown error occurred", exception)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(InternalServerErrorResponse(exception.message))
    }

    @ApiResponses(
        ApiResponse(
            responseCode = BadRequestErrorResponse.CODE,
            description = "Bad Request - Invalid argument",
            content = arrayOf(Content(schema = Schema(implementation = BadRequestErrorResponse::class)))
        )
    )
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val validationErrors: MutableMap<String, String> = HashMap()

        ex.bindingResult.fieldErrors.forEach { error: FieldError ->
            validationErrors[error.field] = error.defaultMessage ?: ""
        }

        val errorMessage = validationErrors.values.toString()

        return ResponseEntity
            .status(status)
            .body(BadRequestErrorResponse(errorMessage))
    }
}

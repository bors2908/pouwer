package ru.itmo.enterprise.common.exception

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import java.nio.file.AccessDeniedException
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
import ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse
import ru.itmo.enterprise.common.exception.handler.response.ConflictErrorResponse
import ru.itmo.enterprise.common.exception.handler.response.ForbiddenErrorResponse
import ru.itmo.enterprise.common.exception.handler.response.InternalServerErrorResponse
import ru.itmo.enterprise.common.exception.handler.response.NotFoundErrorResponse
import ru.itmo.enterprise.common.exception.handler.response.UnauthorizedErrorResponse

@ControllerAdvice
class ErrorHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponses(
        ApiResponse(
            responseCode = ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse.CODE,
            description = "Bad Request - Invalid argument",
            content = arrayOf(Content(schema = Schema(implementation = ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse::class)))
        )
    )
    fun handleIllegalArgumentException(exception: IllegalArgumentException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse(exception.message))
    }

    @ExceptionHandler(ru.itmo.enterprise.common.exception.InsufficientPermissionsException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ApiResponses(
        ApiResponse(
            responseCode = ru.itmo.enterprise.common.exception.handler.response.ForbiddenErrorResponse.CODE,
            description = "Forbidden - Access Denied",
            content = arrayOf(Content(schema = Schema(implementation = ru.itmo.enterprise.common.exception.handler.response.ForbiddenErrorResponse::class)))
        )
    )
    fun handleInsufficientPermissionsException(exception: ru.itmo.enterprise.common.exception.InsufficientPermissionsException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ru.itmo.enterprise.common.exception.handler.response.ForbiddenErrorResponse(exception.message))
    }

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ApiResponses(
        ApiResponse(
            responseCode = ru.itmo.enterprise.common.exception.handler.response.UnauthorizedErrorResponse.CODE,
            description = "Unauthorized - Access Denied",
            content = arrayOf(Content(schema = Schema(implementation = ru.itmo.enterprise.common.exception.handler.response.UnauthorizedErrorResponse::class)))
        )
    )
    fun handleAccessDeniedException(exception: AccessDeniedException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ru.itmo.enterprise.common.exception.handler.response.UnauthorizedErrorResponse(exception.message))
    }

    @ExceptionHandler(ru.itmo.enterprise.common.exception.DataNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ApiResponses(
        ApiResponse(
            responseCode = ru.itmo.enterprise.common.exception.handler.response.NotFoundErrorResponse.CODE,
            description = "Not Found - Data not found",
            content = arrayOf(Content(schema = Schema(implementation = ru.itmo.enterprise.common.exception.handler.response.NotFoundErrorResponse::class)))
        )
    )
    fun handleNotFoundException(exception: ru.itmo.enterprise.common.exception.DataNotFoundException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ru.itmo.enterprise.common.exception.handler.response.NotFoundErrorResponse(exception.message))
    }

    @ExceptionHandler(ru.itmo.enterprise.common.exception.DataConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ApiResponses(
        ApiResponse(
            responseCode = ru.itmo.enterprise.common.exception.handler.response.ConflictErrorResponse.CODE,
            description = "Conflict - Data conflict",
            content = arrayOf(Content(schema = Schema(implementation = ru.itmo.enterprise.common.exception.handler.response.ConflictErrorResponse::class)))
        )
    )
    fun handleDataConflictException(exception: ru.itmo.enterprise.common.exception.DataConflictException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ru.itmo.enterprise.common.exception.handler.response.ConflictErrorResponse(exception.message))
    }

    @ExceptionHandler(ru.itmo.enterprise.common.exception.InvalidRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponses(
        ApiResponse(
            responseCode = ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse.CODE,
            description = "Bad Request - Invalid argument",
            content = arrayOf(Content(schema = Schema(implementation = ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse::class)))
        )
    )
    fun handleInvalidRequestException(exception: ru.itmo.enterprise.common.exception.InvalidRequestException): ResponseEntity<Any> {
        logger.warn(exception.message, exception)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse(exception.message))
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ApiResponses(
        ApiResponse(
            responseCode = ru.itmo.enterprise.common.exception.handler.response.InternalServerErrorResponse.CODE,
            description = "Internal Server Error - Unknown error occurred",
            content = arrayOf(Content(schema = Schema(implementation = ru.itmo.enterprise.common.exception.handler.response.InternalServerErrorResponse::class)))
        )
    )
    fun handleAllUncaughtException(exception: Exception): ResponseEntity<Any> {
        logger.error("Unknown error occurred", exception)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ru.itmo.enterprise.common.exception.handler.response.InternalServerErrorResponse(exception.message))
    }

    @ApiResponses(
        ApiResponse(
            responseCode = ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse.CODE,
            description = "Bad Request - Invalid argument",
            content = arrayOf(Content(schema = Schema(implementation = ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse::class)))
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
            validationErrors[error.field] = error.defaultMessage?.toString() ?: ""
        }

        val errorMessage = validationErrors.values.toString()

        return ResponseEntity
            .status(status)
            .body(ru.itmo.enterprise.common.exception.handler.response.BadRequestErrorResponse(errorMessage))
    }
}

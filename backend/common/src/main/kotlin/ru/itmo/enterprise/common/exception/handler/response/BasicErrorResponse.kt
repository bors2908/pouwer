package ru.itmo.enterprise.common.exception.handler.response

data class BasicErrorResponse(
    override val code: String?,
    override val message: String?
) : ru.itmo.enterprise.common.exception.handler.response.ErrorResponse

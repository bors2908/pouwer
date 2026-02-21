package ru.itmo.enterprise.common.exception.handler.response

interface ErrorResponse {
    val code: String?
    val message: String?
}

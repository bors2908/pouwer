package ru.itmo.enterprise.api

import org.springframework.http.ResponseEntity

abstract class ApiBase {
    protected inline fun <reified D> ResponseEntity<D>.getBodySafely(): D {
        return this.body ?: throw AssertionError("Empty Body")
    }
}

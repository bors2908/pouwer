package ge.becrin.pouwer.api

import org.springframework.http.ResponseEntity

abstract class ApiBase {
    protected inline fun <reified D : Any> ResponseEntity<D>.getBodySafely(): D {
        return this.body ?: throw AssertionError("Empty Body")
    }
}

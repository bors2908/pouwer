package ge.becrin.pouwer.pow.controller

import ge.becrin.pouwer.pow.service.StaticPluginProxyService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@RestController
class StaticPluginProxyController(
    private val proxyService: StaticPluginProxyService
) {
    @RequestMapping("/static/{pluginId}", "/static/{pluginId}/**")
    fun proxy(
        @PathVariable pluginId: String,
        request: HttpServletRequest
    ): ResponseEntity<StreamingResponseBody> {
        val (statusCode, headers, body) = proxyService.proxy(pluginId, request)
        val responseHeaders = HttpHeaders().apply {
            putAll(headers)
            add(COOP_HEADER, COOP_VALUE)
            add(COEP_HEADER, COEP_VALUE)
        }
        return ResponseEntity
            .status(HttpStatusCode.valueOf(statusCode))
            .headers(responseHeaders)
            .body(body)
    }

    private companion object {
        private const val COOP_HEADER = "Cross-Origin-Opener-Policy"
        private const val COEP_HEADER = "Cross-Origin-Embedder-Policy"
        private const val COOP_VALUE = "same-origin"
        private const val COEP_VALUE = "require-corp"
    }
}

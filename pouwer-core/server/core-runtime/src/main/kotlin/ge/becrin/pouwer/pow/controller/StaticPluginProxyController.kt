package ge.becrin.pouwer.pow.controller

import ge.becrin.pouwer.pow.service.StaticPluginProxyService
import jakarta.servlet.http.HttpServletRequest
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
        return ResponseEntity
            .status(HttpStatusCode.valueOf(statusCode))
            .headers(headers)
            .body(body)
    }
}

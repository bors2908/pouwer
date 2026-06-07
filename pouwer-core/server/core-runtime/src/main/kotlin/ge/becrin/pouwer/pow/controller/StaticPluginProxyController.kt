package ge.becrin.pouwer.pow.controller

import ge.becrin.pouwer.challenge.api.REST_VALUE
import ge.becrin.pouwer.pow.service.PayloadPluginRegistry
import ge.becrin.pouwer.pow.service.CorePluginLifecycleRegistry
import ge.becrin.pouwer.pow.service.StaticPluginProxyService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@RestController
@ConditionalOnProperty(
    prefix = "pouwer.plugin-transport",
    name = ["mode"],
    havingValue = REST_VALUE,
    matchIfMissing = true
)
class StaticPluginProxyController(
    private val proxyService: StaticPluginProxyService,
    private val payloadPluginRegistry: PayloadPluginRegistry,
    private val remotePluginRegistry: CorePluginLifecycleRegistry? = null,
    @param:Value($$"\${challenge.plugins.priority-override:}")
    private val priorityOverrideRaw: String = ""
) {
    @RequestMapping("/static/{pluginId}", "/static/{pluginId}/**")
    fun proxy(
        @PathVariable pluginId: String,
        request: HttpServletRequest
    ): ResponseEntity<StreamingResponseBody> =
        doProxy(pluginId, request)

    @RequestMapping("/static", "/static/**")
    fun proxyResolved(
        @RequestParam(required = false) pluginId: String?,
        request: HttpServletRequest
    ): ResponseEntity<StreamingResponseBody> {
        val resolved = resolveCandidatePluginIds(pluginId).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No available plugin")

        return doProxy(resolved, request)
    }

    fun resolveCandidatePluginIds(requestedPluginId: String?): List<String> {
        if (!requestedPluginId.isNullOrBlank()) {
            return listOf(requestedPluginId)
        }

        val available = payloadPluginRegistry.pluginIds()
        if (available.isEmpty()) {
            return emptyList()
        }

        val ordered = linkedSetOf<String>()
        val priorityOverride = priorityOverrideRaw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        priorityOverride
            .filter { available.contains(it) }
            .forEach { ordered.add(it) }

        remotePluginRegistry
            ?.getHealthy()
            ?.sortedByDescending { it.registeredAt }
            ?.map { it.id }
            ?.filter { available.contains(it) }
            ?.forEach { ordered.add(it) }

        available
            .filterNot { ordered.contains(it) }
            .forEach { ordered.add(it) }

        return ordered.toList()
    }

    private fun doProxy(pluginId: String, request: HttpServletRequest): ResponseEntity<StreamingResponseBody> {
        val (statusCode, headers, body) = proxyService.proxy(pluginId, request)

        return ResponseEntity
            .status(HttpStatusCode.valueOf(statusCode))
            .headers(headers)
            .body(body)
    }
}

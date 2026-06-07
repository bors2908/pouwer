package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Collections

data class StaticProxyResponse(
    val statusCode: Int,
    val headers: HttpHeaders,
    val body: StreamingResponseBody
)

@Service
class StaticPluginProxyService(
    private val registry: CorePluginLifecycleRegistry,
    private val httpClient: HttpClient
) {
    fun proxy(
        pluginId: String,
        request: HttpServletRequest,
        additionalRequestHeaders: Map<String, String> = emptyMap()
    ): StaticProxyResponse {
        val plugin = registry.get(pluginId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin not found: $pluginId")

        if (plugin.status != PluginStatus.HEALTHY) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Plugin unavailable: $pluginId")
        }

        val upstreamRequest = buildRequest(plugin, pluginId, request, additionalRequestHeaders)
        val upstreamResponse = try {
            httpClient.send(upstreamRequest, HttpResponse.BodyHandlers.ofInputStream())
        } catch (e: Exception) {
            log.error(e) { "Static proxy failed for plugin $pluginId" }
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Plugin $pluginId failed", e)
        }

        return StaticProxyResponse(
            statusCode = upstreamResponse.statusCode(),
            headers = copyResponseHeaders(upstreamResponse.headers()),
            body = { output ->
                upstreamResponse.body().use { input ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }
        )
    }

    private fun buildRequest(
        plugin: PluginMetadata,
        pluginId: String,
        request: HttpServletRequest,
        additionalRequestHeaders: Map<String, String>
    ): HttpRequest {
        val uri = buildUpstreamUri(plugin.baseUrl, request, pluginId)
        val builder = HttpRequest.newBuilder(uri)

        val headerNames = request.headerNames?.let { Collections.list(it) }.orEmpty()
        headerNames.forEach { headerName ->
            if (!isHopByHopHeader(headerName) && !isExcludedRequestHeader(headerName)) {
                Collections.list(request.getHeaders(headerName)).forEach { value ->
                    builder.header(headerName, value)
                }
            }
        }

        additionalRequestHeaders.forEach { (headerName, value) ->
            if (!isHopByHopHeader(headerName) && !isExcludedRequestHeader(headerName)) {
                builder.setHeader(headerName, value)
            }
        }

        val method = request.method.uppercase()
        val bodyPublisher = when {
            method == "GET" || method == "HEAD" -> HttpRequest.BodyPublishers.noBody()
            request.contentLengthLong == 0L && request.inputStream.available() == 0 -> HttpRequest.BodyPublishers.noBody()
            else -> HttpRequest.BodyPublishers.ofInputStream { request.inputStream }
        }

        return builder.method(method, bodyPublisher).build()
    }

    private fun buildUpstreamUri(baseUrl: String, request: HttpServletRequest, pluginId: String): URI {
        val contextPath = request.contextPath.orEmpty()
        val requestUri = request.requestURI.removePrefix(contextPath)
        val prefix = "/static/$pluginId"
        val forwardPath = requestUri
            .removePrefix(prefix)
            .ifBlank { "/" }
            .let { if (it.startsWith("/")) it else "/$it" }

        return UriComponentsBuilder.fromUriString(baseUrl)
            .path(forwardPath)
            .apply {
                request.queryString?.let { query(it) }
            }
            .build(true)
            .toUri()
    }

    private fun copyResponseHeaders(responseHeaders: java.net.http.HttpHeaders?): HttpHeaders {
        if (responseHeaders == null) {
            return HttpHeaders()
        }

        val headers = HttpHeaders()

        responseHeaders.map().forEach { (name, values) ->
            if (!isHopByHopHeader(name) && !isExcludedResponseHeader(name)) {
                headers.addAll(name, values)
            }
        }
        return headers
    }

    private fun isHopByHopHeader(headerName: String): Boolean {
        return headerName.equals("connection", ignoreCase = true) ||
            headerName.equals("keep-alive", ignoreCase = true) ||
            headerName.equals("proxy-authenticate", ignoreCase = true) ||
            headerName.equals("proxy-authorization", ignoreCase = true) ||
            headerName.equals("te", ignoreCase = true) ||
            headerName.equals("trailer", ignoreCase = true) ||
            headerName.equals("transfer-encoding", ignoreCase = true) ||
            headerName.equals("upgrade", ignoreCase = true) ||
            headerName.equals("host", ignoreCase = true)
    }

    private fun isExcludedRequestHeader(headerName: String): Boolean {
        return headerName.equals("content-length", ignoreCase = true)
    }

    private fun isExcludedResponseHeader(headerName: String): Boolean {
        return headerName.equals("content-length", ignoreCase = true)
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024
        private val log = KotlinLogging.logger {}
    }
}

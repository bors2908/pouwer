package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StaticPluginProxyServiceTest {
    private var server: com.sun.net.httpserver.HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
        server = null
    }

    @Test
    fun `proxy forwards request path headers body and upstream response`() {
        val seenPath = AtomicReference<String?>()
        val seenHeader = AtomicReference<String?>()
        val seenBody = AtomicReference<String?>()

        val httpServer = startServer { exchange ->
            seenPath.set(exchange.requestURI.path)
            seenHeader.set(exchange.requestHeaders.getFirst("X-Test-Header"))
            seenBody.set(exchange.requestBody.readBytes().decodeToString())
            exchange.responseHeaders.add("X-Upstream", "yes")
            exchange.sendResponseHeaders(201, 5)
            exchange.responseBody.use { it.write("hello".toByteArray()) }
        }

        val service = createService(httpServer)
        val request = MockHttpServletRequest("POST", "/static/test-plugin/assets/app.js")
            .apply {
                setContent("payload".toByteArray())
                addHeader("X-Test-Header", "forward-me")
                contentType = "text/plain"
            }

        val proxy = service.proxy("test-plugin", request)
        val output = ByteArrayOutputStream()
        proxy.body.writeTo(output)

        assertEquals("/assets/app.js", seenPath.get())
        assertEquals("forward-me", seenHeader.get())
        assertEquals("payload", seenBody.get())
        assertEquals(HttpStatus.CREATED.value(), proxy.statusCode)
        assertEquals("yes", proxy.headers.getFirst("X-Upstream"))
        assertEquals("hello", output.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun `proxy fails when plugin is missing`() {
        val service = StaticPluginProxyService(RemotePluginRegistry(), java.net.http.HttpClient.newHttpClient())
        val request = MockHttpServletRequest("GET", "/static/missing/app.js")

        val exception = assertFailsWith<ResponseStatusException> {
            service.proxy("missing", request)
        }

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.statusCode.value())
    }

    private fun createService(httpServer: com.sun.net.httpserver.HttpServer): StaticPluginProxyService {
        val registry = RemotePluginRegistry()
        registry.register(
            PluginMetadata(
                id = "test-plugin",
                version = "1.0.0",
                contractVersion = "1.0.0",
                baseUrl = "http://localhost:${httpServer.address.port}",
                lastHeartbeat = Instant.now(),
                status = PluginStatus.HEALTHY,
                registeredAt = Instant.now()
            )
        )
        return StaticPluginProxyService(registry, java.net.http.HttpClient.newHttpClient())
    }

    private fun startServer(handler: (com.sun.net.httpserver.HttpExchange) -> Unit): com.sun.net.httpserver.HttpServer {
        val httpServer = com.sun.net.httpserver.HttpServer.create(InetSocketAddress(0), 0)
        httpServer.createContext("/") { exchange -> handler(exchange) }
        httpServer.executor = Executors.newCachedThreadPool()
        httpServer.start()
        server = httpServer
        return httpServer
    }
}

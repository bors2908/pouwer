package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.PluginHeartbeat
import ge.becrin.pouwer.challenge.api.PluginRegistration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class RestPluginCoreTransportAdapterTest {

    @Test
    fun testRegisterSucceedsOn2xx() {
        val restTemplate = TestRestTemplate(HttpStatus.OK)
        val adapter = RestPluginCoreTransportAdapter(PluginBaseProperties(url = "http://core:8082"), restTemplate)

        adapter.register(PluginRegistration("plugin-1", "1.0.0", "v1", "http://localhost:8081"))

        assertEquals("http://core:8082/core/plugins/register", restTemplate.lastUrl)
        val body = restTemplate.lastBody as PluginRegistration
        assertEquals("plugin-1", body.id)
        assertEquals("http://localhost:8081", body.baseUrl)
    }

    @Test
    fun testRegisterThrowsOnNon2xx() {
        val restTemplate = TestRestTemplate(HttpStatus.BAD_REQUEST)
        val adapter = RestPluginCoreTransportAdapter(PluginBaseProperties(url = "http://core:8082"), restTemplate)

        assertThrows(IllegalStateException::class.java) {
            adapter.register(PluginRegistration("plugin-1", "1.0.0", "v1", "http://localhost:8081"))
        }
    }

    @Test
    fun testSendHeartbeatSucceedsOn2xx() {
        val restTemplate = TestRestTemplate(HttpStatus.OK)
        val adapter = RestPluginCoreTransportAdapter(PluginBaseProperties(url = "http://core:8082"), restTemplate)

        adapter.heartbeat(PluginHeartbeat("plugin-1", java.time.Instant.now()))

        assertEquals("http://core:8082/core/plugins/plugin-1/heartbeat", restTemplate.lastUrl)
        assertEquals("plugin-1", (restTemplate.lastBody as PluginHeartbeat).id)
    }

    @Test
    fun testSendHeartbeatThrowsOnNon2xx() {
        val restTemplate = TestRestTemplate(HttpStatus.SERVICE_UNAVAILABLE)
        val adapter = RestPluginCoreTransportAdapter(PluginBaseProperties(url = "http://core:8082"), restTemplate)

        assertThrows(IllegalStateException::class.java) {
            adapter.heartbeat(PluginHeartbeat("plugin-1", java.time.Instant.now()))
        }
    }

    private class TestRestTemplate(
        private val status: HttpStatus
    ) : RestTemplate() {
        var lastUrl: String? = null
        var lastBody: Any? = null

        override fun <T : Any> postForEntity(
            url: String,
            request: Any?,
            responseType: Class<T>,
            vararg uriVariables: Any?
        ): ResponseEntity<T> {
            lastUrl = url
            lastBody = request
            return ResponseEntity.status(status).build()
        }
    }
}

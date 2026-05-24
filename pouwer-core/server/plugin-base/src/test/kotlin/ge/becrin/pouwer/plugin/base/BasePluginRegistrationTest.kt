package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.PluginHeartbeat
import ge.becrin.pouwer.challenge.api.PluginRegistration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class BasePluginRegistrationTest {

    @Test
    fun testRegisterPluginSucceedsOn2xx() {
        val restTemplate = TestRestTemplate(HttpStatus.OK)
        val registration = TestRegistration(
            properties = PluginBaseProperties(url = "http://core:8082"),
            pluginVersion = "1.0.0",
            httpClient = restTemplate
        )

        registration.registerPlugin()

        assertEquals("http://core:8082/core/plugins/register", restTemplate.lastUrl)
        val body = restTemplate.lastBody as PluginRegistration
        assertEquals("plugin-1", body.id)
        assertEquals("http://localhost:8081", body.baseUrl)
    }

    @Test
    fun testRegisterPluginThrowsOnNon2xx() {
        val restTemplate = TestRestTemplate(HttpStatus.BAD_REQUEST)
        val registration = TestRegistration(
            properties = PluginBaseProperties(url = "http://core:8082"),
            pluginVersion = "1.0.0",
            httpClient = restTemplate
        )

        assertThrows(IllegalStateException::class.java) {
            registration.registerPlugin()
        }
    }

    @Test
    fun testSendHeartbeatSucceedsOn2xx() {
        val restTemplate = TestRestTemplate(HttpStatus.OK)
        val registration = TestRegistration(
            properties = PluginBaseProperties(url = "http://core:8082"),
            pluginVersion = "1.0.0",
            httpClient = restTemplate
        )

        registration.sendHeartbeat()

        assertEquals("http://core:8082/core/plugins/plugin-1/heartbeat", restTemplate.lastUrl)
        assertNotNull(restTemplate.lastBody as PluginHeartbeat)
    }

    @Test
    fun testSendHeartbeatThrowsOnNon2xx() {
        val restTemplate = TestRestTemplate(HttpStatus.SERVICE_UNAVAILABLE)
        val registration = TestRegistration(
            properties = PluginBaseProperties(url = "http://core:8082"),
            pluginVersion = "1.0.0",
            httpClient = restTemplate
        )

        assertThrows(IllegalStateException::class.java) {
            registration.sendHeartbeat()
        }
    }

    @Test
    fun testRegisterPluginUsesOverriddenBaseUrl() {
        val restTemplate = TestRestTemplate(HttpStatus.OK)
        val registration = OverriddenBaseUrlRegistration(
            properties = PluginBaseProperties(url = "http://core:8082"),
            pluginVersion = "1.0.0",
            httpClient = restTemplate
        )

        registration.registerPlugin()

        val body = restTemplate.lastBody as PluginRegistration
        assertEquals("https://plugin.example.test", body.baseUrl)
    }

    private class TestRegistration(
        properties: PluginBaseProperties,
        pluginVersion: String,
        httpClient: RestTemplate
    ) : BasePluginRegistration(properties, pluginVersion, httpClient) {
        override fun getPluginId(): String = "plugin-1"
        override fun getPluginPort(): Int = 8081
        override fun getPluginHost(): String = "http://localhost"
    }

    private class OverriddenBaseUrlRegistration(
        properties: PluginBaseProperties,
        pluginVersion: String,
        httpClient: RestTemplate
    ) : BasePluginRegistration(properties, pluginVersion, httpClient) {
        override fun getPluginId(): String = "plugin-1"
        override fun getPluginPort(): Int = 8081
        override fun getPluginHost(): String = "http://localhost"
        override fun getBaseUrl(): String = "https://plugin.example.test"
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

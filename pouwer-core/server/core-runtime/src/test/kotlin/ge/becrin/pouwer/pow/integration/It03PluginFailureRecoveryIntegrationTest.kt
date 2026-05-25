package ge.becrin.pouwer.pow.integration

import ge.becrin.pouwer.IntegrationTestBase
import ge.becrin.pouwer.challenge.api.PluginStatus
import ge.becrin.pouwer.pow.service.PayloadPluginRegistry
import ge.becrin.pouwer.pow.service.RemotePluginRegistry
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(It03PluginFailureRecoveryIntegrationTest.MockMvcTestConfiguration::class)
class It03PluginFailureRecoveryIntegrationTest : IntegrationTestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var payloadPluginRegistry: PayloadPluginRegistry

    @Autowired
    private lateinit var remotePluginRegistry: RemotePluginRegistry

    private lateinit var flakyPlugin: MockWebServer
    private lateinit var stablePlugin: MockWebServer
    private val flakyBuildFails = AtomicBoolean(false)

    @BeforeEach
    fun setUp() {
        flakyPlugin = MockWebServer()
        stablePlugin = MockWebServer()
        flakyPlugin.dispatcher = pluginDispatcher("it03-flaky", flakyBuildFails)
        stablePlugin.dispatcher = pluginDispatcher("it03-stable", AtomicBoolean(false))
        flakyPlugin.start()
        stablePlugin.start()
    }

    @AfterEach
    fun tearDown() {
        if (::flakyPlugin.isInitialized) flakyPlugin.shutdown()
        if (::stablePlugin.isInitialized) stablePlugin.shutdown()
    }

    @Disabled
    @Test
    fun pluginFailureMarksUnhealthyAndHeartbeatRecoversRouting() {
        registerPlugin("it03-stable", stablePlugin.url("/").toString().removeSuffix("/"))
        registerPlugin("it03-flaky", flakyPlugin.url("/").toString().removeSuffix("/"))
        payloadPluginRegistry.refresh()

        val firstTask = challenge("it03-flaky")
        assertEquals("it03-flaky", firstTask.get("pluginId").asText())

        flakyBuildFails.set(true)
        val fallbackTask = challenge("it03-stable")
        assertEquals("it03-stable", fallbackTask.get("pluginId").asText())

        val unhealthy = remotePluginRegistry.get("it03-flaky")
        assertNotNull(unhealthy)
        assertEquals(PluginStatus.UNHEALTHY, unhealthy!!.status)

        flakyBuildFails.set(false)
        sendHeartbeat("it03-flaky")
        payloadPluginRegistry.refresh()

        val recoveredTask = challenge("it03-stable")
        assertEquals("it03-flaky", recoveredTask.get("pluginId").asText())
    }

    private fun pluginDispatcher(pluginId: String, failBuild: AtomicBoolean): Dispatcher =
        object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path?.substringBefore('?')) {
                    "/plugin/supports" -> json(200, "true")
                    "/plugin/payload/build" -> {
                        if (failBuild.get()) {
                            MockResponse().setResponseCode(500)
                        } else {
                            json(
                                200,
                                """
                                {
                                  "jobId":"11111111-1111-1111-1111-111111111111",
                                  "pluginId":"$pluginId",
                                  "expiresAt":4102444800000,
                                  "payload":{"kind":"test"}
                                }
                                """.trimIndent()
                            )
                        }
                    }
                    "/plugin/payload/validate" -> json(200, """{"status":"ACCEPTED"}""")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

    private fun registerPlugin(pluginId: String, baseUrl: String) {
        val body = objectMapper.writeValueAsString(
            mapOf(
                "id" to pluginId,
                "version" to "1.0.0",
                "contractVersion" to "0.2.0",
                "baseUrl" to baseUrl
            )
        )
        val response = httpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$port/core/plugins/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertEquals(200, response.statusCode())
    }

    private fun sendHeartbeat(pluginId: String) {
        val body = objectMapper.writeValueAsString(
            mapOf(
                "id" to pluginId,
                "timestamp" to Instant.now().truncatedTo(ChronoUnit.MILLIS).toString(),
                "status" to "ok"
            )
        )
        val response = httpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$port/core/plugins/$pluginId/heartbeat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertEquals(200, response.statusCode())
    }

    private fun challenge(pluginId: String): tools.jackson.databind.JsonNode {
        val response = httpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$port/challenge?pluginId=$pluginId"))
                .header("Accept", "application/json")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertEquals(200, response.statusCode())
        return objectMapper.readTree(response.body())
    }

    private fun json(status: Int, body: String): MockResponse =
        MockResponse()
            .setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body)

    private fun httpClient(): HttpClient = HttpClient.newHttpClient()

    @TestConfiguration
    open class MockMvcTestConfiguration {
        @Bean
        open fun mockMvc(webApplicationContext: WebApplicationContext): MockMvc =
            MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }
}

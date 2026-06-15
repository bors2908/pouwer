package ge.becrin.pouwer.pow.integration

import ge.becrin.pouwer.IntegrationTestBase
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import ge.becrin.pouwer.pow.service.PayloadPluginRegistry
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "challenge.plugins.priority-override=it04-failing",
        "resilience4j.circuitbreaker.instances.pluginTransport.slidingWindowSize=2",
        "resilience4j.circuitbreaker.instances.pluginTransport.minimumNumberOfCalls=2",
        "resilience4j.circuitbreaker.instances.pluginTransport.failureRateThreshold=50",
        "resilience4j.circuitbreaker.instances.pluginTransport.waitDurationInOpenState=60000"
    ]
)
@Import(It04CircuitBreakerIntegrationTest.MockMvcTestConfiguration::class)
class It04CircuitBreakerIntegrationTest : IntegrationTestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Autowired
    private lateinit var payloadPluginRegistry: PayloadPluginRegistry

    private lateinit var failingPlugin: MockWebServer
    private val validateCalls = AtomicInteger(0)

    @BeforeEach
    fun setUp() {
        failingPlugin = MockWebServer()
        failingPlugin.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path?.substringBefore('?')) {
                    "/plugin/supports" -> json(200, "true")
                    "/plugin/payload/build" -> json(
                        200,
                        """
                        {
                          "jobId":"22222222-2222-2222-2222-222222222222",
                          "pluginId":"it04-failing",
                          "expiresAt":4102444800000,
                          "payload":{"kind":"test"}
                        }
                        """.trimIndent()
                    )
                    "/plugin/payload/validate" -> {
                        validateCalls.incrementAndGet()
                        MockResponse().setResponseCode(500)
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        failingPlugin.start()
    }

    @AfterEach
    fun tearDown() {
        if (::failingPlugin.isInitialized) failingPlugin.shutdown()
    }

    @Test
    fun opensCircuitBreakerAndStopsCallingPluginValidate() {
        registerPlugin("it04-failing", failingPlugin.url("/").toString().removeSuffix("/"))
        payloadPluginRegistry.refresh()
        val task = challenge("it04-failing")
        assertEquals("it04-failing", task.get("pluginId").asText())

        val attempts = 6
        repeat(attempts) { index ->
            val response = validate(
                jobId = task.get("jobId").asText(),
                pluginId = "it04-failing",
                nonce = index.toLong()
            )
            assertEquals(422, response.statusCode())
        }

        val state = circuitBreakerRegistry.circuitBreaker("pluginTransport").state
        assertEquals(CircuitBreaker.State.OPEN, state)

        val callsBeforeExtraAttempts = validateCalls.get()
        repeat(3) { index ->
            val response = validate(
                jobId = task.get("jobId").asText(),
                pluginId = "it04-failing",
                nonce = (attempts + index).toLong()
            )
            assertEquals(422, response.statusCode())
        }
        val callsAfterExtraAttempts = validateCalls.get()
        assertEquals(callsBeforeExtraAttempts, callsAfterExtraAttempts)
        val notPermittedCalls = circuitBreakerRegistry
            .circuitBreaker("pluginTransport")
            .metrics
            .numberOfNotPermittedCalls
        assertTrue(notPermittedCalls > 0)
    }

    private fun registerPlugin(pluginId: String, baseUrl: String) {
        val body = objectMapper.writeValueAsString(
            mapOf(
                "id" to pluginId,
                "version" to "1.0.0",
                "contractVersion" to "0.2.3",
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

    private fun challenge(pluginId: String): tools.jackson.databind.JsonNode {
        val response = httpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$port/challenge?pluginId=${pluginId}"))
                .header("Accept", "application/json")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertEquals(200, response.statusCode())
        return objectMapper.readTree(response.body())
    }

    private fun validate(jobId: String, pluginId: String, nonce: Long): HttpResponse<String> {
        val body = objectMapper.writeValueAsString(
            mapOf(
                "jobId" to jobId,
                "pluginId" to pluginId,
                "payload" to mapOf(
                    "nonce" to nonce,
                    "hashHex" to UUID.randomUUID().toString().replace("-", "")
                ),
                "durationMs" to 1,
                "attempts" to 1
            )
        )
        return httpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$port/validate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
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

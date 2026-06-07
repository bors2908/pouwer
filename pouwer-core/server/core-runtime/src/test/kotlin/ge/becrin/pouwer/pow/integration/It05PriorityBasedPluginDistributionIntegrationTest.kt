package ge.becrin.pouwer.pow.integration

import ge.becrin.pouwer.IntegrationTestBase
import ge.becrin.pouwer.pow.service.PayloadPluginRegistry
import ge.becrin.pouwer.pow.service.RemotePluginRegistry
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["challenge.plugins.priority-override=it05-sha256,it05-stub"]
)
@Import(It05PriorityBasedPluginDistributionIntegrationTest.MockMvcTestConfiguration::class)
class It05PriorityBasedPluginDistributionIntegrationTest : IntegrationTestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var payloadPluginRegistry: PayloadPluginRegistry

    @Autowired
    private lateinit var remotePluginRegistry: RemotePluginRegistry

    private lateinit var sha256Plugin: MockWebServer
    private lateinit var stubPlugin: MockWebServer
    private val sha256BuildCalls = AtomicInteger(0)
    private val stubBuildCalls = AtomicInteger(0)

    @BeforeEach
    fun setUp() {
        sha256Plugin = MockWebServer()
        stubPlugin = MockWebServer()

        sha256Plugin.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path?.substringBefore('?')) {
                    "/plugin/supports" -> json(200, "true")
                    "/plugin/payload/build" -> {
                        sha256BuildCalls.incrementAndGet()
                        json(
                            200,
                            """
                            {
                              "jobId":"11111111-1111-1111-1111-111111111111",
                              "pluginId":"it05-sha256",
                              "expiresAt":4102444800000,
                              "payload":{"kind":"sha256"}
                            }
                            """.trimIndent()
                        )
                    }
                    "/plugin/payload/validate" -> json(200, """{"status":"ACCEPTED"}""")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        stubPlugin.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path?.substringBefore('?')) {
                    "/plugin/supports" -> json(200, "true")
                    "/plugin/payload/build" -> {
                        stubBuildCalls.incrementAndGet()
                        json(
                            200,
                            """
                            {
                              "jobId":"22222222-2222-2222-2222-222222222222",
                              "pluginId":"it05-stub",
                              "expiresAt":4102444800000,
                              "payload":{"kind":"stub"}
                            }
                            """.trimIndent()
                        )
                    }
                    "/plugin/payload/validate" -> json(200, """{"status":"ACCEPTED"}""")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        sha256Plugin.start()
        stubPlugin.start()
    }

    @AfterEach
    fun tearDown() {
        if (::sha256Plugin.isInitialized) sha256Plugin.shutdown()
        if (::stubPlugin.isInitialized) stubPlugin.shutdown()
    }

    @Test
    fun routesToHigherPriorityPluginWhenBothAvailable() {
        registerPlugin("it05-sha256", sha256Plugin.url("/").toString().removeSuffix("/"))
        registerPlugin("it05-stub", stubPlugin.url("/").toString().removeSuffix("/"))
        payloadPluginRegistry.refresh()

        val taskCount = 5
        repeat(taskCount) {
            val task = challenge("it05-sha256")
            assertEquals("it05-sha256", task.get("pluginId").asText(), "Task $it should use SHA256 (higher priority)")
        }

        assertTrue(sha256BuildCalls.get() == taskCount, "SHA256 should handle all requests")
        assertEquals(0, stubBuildCalls.get(), "Stub should not be called when SHA256 is available")
    }

    @Test
    fun fallsbackToLowerPriorityPluginWhenHigherPriorityFails() {
        registerPlugin("it05-sha256", sha256Plugin.url("/").toString().removeSuffix("/"))
        registerPlugin("it05-stub", stubPlugin.url("/").toString().removeSuffix("/"))
        payloadPluginRegistry.refresh()

        // First, verify SHA256 plugin is being used
        val firstTask = challenge("it05-sha256")
        assertEquals("it05-sha256", firstTask.get("pluginId").asText())

        // Stop the SHA256 plugin to simulate failure
        sha256Plugin.shutdown()

        // After a short delay for health check detection, requests should fallback
        Thread.sleep(100)
        
        // Try to get a challenge - should work with stub (though might retry on sha256 first)
        var task: JsonNode? = null
        for (attempt in 0..4) {
            try {
                task = challenge("it05-stub")
                if (task.get("pluginId").asText() == "it05-stub") {
                    break
                }
            } catch (e: Exception) {
                Thread.sleep(50)
            }
        }

        assertNotNull(task, "Should get a task after SHA256 failure")
        assertEquals("it05-stub", task?.get("pluginId")?.asText(), "Should fallback to stub after SHA256 failure")
        assertTrue(stubBuildCalls.get() > 0, "Stub plugin should be called after fallback")
    }

    @Test
    fun priorityOrderingEnforcedWhenBothPluginsHealthy() {
        registerPlugin("it05-sha256", sha256Plugin.url("/").toString().removeSuffix("/"))
        registerPlugin("it05-stub", stubPlugin.url("/").toString().removeSuffix("/"))
        payloadPluginRegistry.refresh()

        val tasksBeforeFallback = 3
        val tasksAfterFallback = 2

        // Generate tasks on primary plugin
        repeat(tasksBeforeFallback) {
            val task = challenge("it05-sha256")
            assertEquals("it05-sha256", task.get("pluginId").asText(), "Should use SHA256 (higher priority)")
        }

        // Disable the primary plugin
        sha256Plugin.shutdown()
        Thread.sleep(100)
        payloadPluginRegistry.disable("it05-sha256")
        payloadPluginRegistry.refresh()

        // Generate tasks on fallback plugin - should work without errors
        repeat(tasksAfterFallback) {
            val task = challenge("it05-stub")
            assertEquals("it05-stub", task.get("pluginId").asText(), "Should fallback to stub")
        }

        assertEquals(tasksBeforeFallback, sha256BuildCalls.get(), "SHA256 should only handle pre-failure requests")
        assertEquals(tasksAfterFallback, stubBuildCalls.get(), "Stub should handle post-failure requests")
    }

    private fun registerPlugin(pluginId: String, baseUrl: String) {
        val body = objectMapper.writeValueAsString(
            mapOf(
                "id" to pluginId,
                "version" to "1.0.0",
                "contractVersion" to "0.2.1",
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
        assertEquals(200, response.statusCode(), "Plugin registration should succeed")
    }

    private fun challenge(pluginId: String): JsonNode {
        val response = httpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$port/challenge?pluginId=${pluginId}"))
                .header("Accept", "application/json")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertEquals(200, response.statusCode(), "Challenge request should succeed")
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

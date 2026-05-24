package ge.becrin.pouwer.pow.integration

import ge.becrin.pouwer.IntegrationTestBase
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.pow.service.PayloadPluginProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["challenge.task.ttl-ms=150"]
)
@Import(It02TaskTtlExpirationIntegrationTest.TestPluginsConfiguration::class)
class It02TaskTtlExpirationIntegrationTest : IntegrationTestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun rejectExpiredTaskAfterTtl() {
        val httpClient = HttpClient.newHttpClient()
        val challengeRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/challenge?pluginId=${It01Sha256PowCycleIntegrationTest.TestSha256Plugin.PLUGIN_ID}"))
            .header("Accept", "application/json")
            .GET()
            .build()
        val challengeResponse = httpClient.send(challengeRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, challengeResponse.statusCode())
        val challengeJson = objectMapper.readTree(challengeResponse.body())
        val jobId = challengeJson.get("jobId").asText()

        Thread.sleep(300)

        val validateBody = objectMapper.writeValueAsString(
            mapOf(
                "jobId" to jobId,
                "pluginId" to It01Sha256PowCycleIntegrationTest.TestSha256Plugin.PLUGIN_ID,
                "payload" to mapOf("nonce" to 0, "hashHex" to "00"),
                "durationMs" to 1,
                "attempts" to 1
            )
        )
        val validateRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/validate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(validateBody))
            .build()
        val validateResponse = httpClient.send(validateRequest, HttpResponse.BodyHandlers.ofString())

        assertEquals(422, validateResponse.statusCode())
        val validateJson = objectMapper.readTree(validateResponse.body())
        assertTrue(validateJson.get("status").asText() == "rejected")
        assertTrue(validateJson.get("reason").asText() == "Task expired")
    }

    @TestConfiguration
    open class TestPluginsConfiguration {
        @Bean
        open fun mockMvc(webApplicationContext: WebApplicationContext): MockMvc =
            MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        @Bean
        @Primary
        open fun testPluginProvider(): PayloadPluginProvider {
            val plugin = It01Sha256PowCycleIntegrationTest.TestSha256Plugin()
            return object : PayloadPluginProvider {
                override fun loadPlugins(): List<PayloadPlugin> = listOf(plugin)
                override fun disable(pluginId: String) = Unit
            }
        }
    }
}

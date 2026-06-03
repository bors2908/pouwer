package ge.becrin.pouwer.pow.integration

import tools.jackson.databind.node.JsonNodeFactory
import ge.becrin.pouwer.IntegrationTestBase
import ge.becrin.pouwer.challenge.api.NonceRange
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import ge.becrin.pouwer.pow.service.PayloadPluginProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Import
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID
import kotlin.random.Random

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(It01Sha256PowCycleIntegrationTest.TestPluginsConfiguration::class)
class It01Sha256PowCycleIntegrationTest : IntegrationTestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val hex = HexFormat.of()

    @Test
    fun fullPowCycleAcceptThenConflictOnReplay() {
        val httpClient = HttpClient.newHttpClient()
        val challengeRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/challenge?pluginId=${TestSha256Plugin.PLUGIN_ID}"))
            .header("Accept", "application/json")
            .GET()
            .build()
        val challengeResponse = httpClient.send(challengeRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, challengeResponse.statusCode())
        val challengeJson = objectMapper.readTree(challengeResponse.body())

        val jobId = UUID.fromString(challengeJson.get("jobId").asText())
        val payload = challengeJson.get("payload")
        val dataHex = payload.get("dataHex").asText()
        val nonceOffset = payload.get("nonceOffset").asInt()
        val nonceIsLE = payload.get("nonceIsLE").asBoolean()
        val targetHex = payload.get("targetHex").asText()
        val nonceStart = payload.get("nonceRange").get("start").asLong()
        val nonceEnd = payload.get("nonceRange").get("end").asLong()

        val solved = solveSha256(
            dataHex = dataHex,
            nonceOffset = nonceOffset,
            nonceIsLE = nonceIsLE,
            targetHex = targetHex,
            start = nonceStart,
            endExclusive = nonceEnd
        )

        val resultMessage: Map<String, Any> = mapOf(
            "jobId" to jobId.toString(),
            "pluginId" to TestSha256Plugin.PLUGIN_ID,
            "payload" to mapOf(
                "nonce" to solved.nonce,
                "hashHex" to solved.hashHex
            ),
            "durationMs" to 10,
            "attempts" to 1
        )

        val requestBody = objectMapper.writeValueAsString(resultMessage)
        val validateRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/validate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val firstValidation = httpClient.send(validateRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, firstValidation.statusCode())
        assertTrue(objectMapper.readTree(firstValidation.body()).get("status").asText() == "accepted")

        val secondValidation = httpClient.send(validateRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(409, secondValidation.statusCode())
        assertTrue(objectMapper.readTree(secondValidation.body()).get("status").asText() == "conflict")
    }

    private fun solveSha256(
        dataHex: String,
        nonceOffset: Int,
        nonceIsLE: Boolean,
        targetHex: String,
        start: Long,
        endExclusive: Long
    ): SolvedResult {
        val target = BigInteger(1, hex.parseHex(targetHex))
        val digest = MessageDigest.getInstance("SHA-256")

        var nonce = start
        while (nonce < endExclusive) {
            val bytes = hex.parseHex(dataHex)
            putNonce(bytes, nonceOffset, nonce.toInt(), nonceIsLE)
            val hash = digest.digest(digest.digest(bytes))
            val hashAsBigInteger = BigInteger(1, hash.reversedArray())
            if (hashAsBigInteger <= target) {
                return SolvedResult(nonce = nonce, hashHex = hex.formatHex(hash))
            }
            nonce++
        }

        throw IllegalStateException("No valid nonce found in test range")
    }

    private fun putNonce(buffer: ByteArray, offset: Int, nonce: Int, littleEndian: Boolean) {
        if (littleEndian) {
            buffer[offset] = (nonce and 0xFF).toByte()
            buffer[offset + 1] = ((nonce shr 8) and 0xFF).toByte()
            buffer[offset + 2] = ((nonce shr 16) and 0xFF).toByte()
            buffer[offset + 3] = ((nonce shr 24) and 0xFF).toByte()
        } else {
            buffer[offset] = ((nonce shr 24) and 0xFF).toByte()
            buffer[offset + 1] = ((nonce shr 16) and 0xFF).toByte()
            buffer[offset + 2] = ((nonce shr 8) and 0xFF).toByte()
            buffer[offset + 3] = (nonce and 0xFF).toByte()
        }
    }

    private data class SolvedResult(
        val nonce: Long,
        val hashHex: String
    )

    @TestConfiguration
    open class TestPluginsConfiguration {
        @Bean
        open fun mockMvc(webApplicationContext: WebApplicationContext): MockMvc =
            MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        @Bean
        @Primary
        open fun testPluginProvider(): PayloadPluginProvider {
            val plugin = TestSha256Plugin()
            return object : PayloadPluginProvider {
                override fun loadPlugins(): List<PayloadPlugin> = listOf(plugin)
                override fun disable(pluginId: String) = Unit
            }
        }
    }

    class TestSha256Plugin : PayloadPlugin {
        private val hex = HexFormat.of()
        private val digest = MessageDigest.getInstance("SHA-256")

        override fun id(): String = PLUGIN_ID

        override fun version(): String = "test"

        override fun buildPayload(request: PayloadBuildRequest): Task {
            val data = ByteArray(32)
            Random.nextBytes(data)
            val payload = JsonNodeFactory.instance.objectNode().apply {
                put("dataHex", hex.formatHex(data))
                put("nonceOffset", 0)
                put("nonceIsLE", false)
                put("targetHex", "0000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
                set("nonceRange", JsonNodeFactory.instance.objectNode().apply {
                    put("start", 0)
                    put("end", 1_000_000)
                })
            }
            return Task(
                jobId = UUID.randomUUID(),
                pluginId = PLUGIN_ID,
                expiresAt = request.nowMillis + request.taskTtlMillis,
                payload = payload
            )
        }

        override fun validateResult(task: Task, result: ResultMessage): ValidationResult {
            val taskPayload = task.payload
            val resultPayload = result.payload

            val nonce = resultPayload.get("nonce")?.asLong() ?: return ValidationResult(ValidationStatus.REJECTED, "Missing nonce")
            val hashHex = resultPayload.get("hashHex")?.asText() ?: return ValidationResult(ValidationStatus.REJECTED, "Missing hash")
            val dataHex = taskPayload.get("dataHex").asText()
            val nonceOffset = taskPayload.get("nonceOffset").asInt()
            val nonceIsLE = taskPayload.get("nonceIsLE").asBoolean()
            val targetHex = taskPayload.get("targetHex").asText()
            val nonceRange = NonceRange(
                start = taskPayload.get("nonceRange").get("start").asLong(),
                end = taskPayload.get("nonceRange").get("end").asLong()
            )

            if (nonce < nonceRange.start || nonce >= nonceRange.end) {
                return ValidationResult(ValidationStatus.CONFLICT, "Nonce outside lease")
            }

            val bytes = hex.parseHex(dataHex)
            putNonce(bytes, nonceOffset, nonce.toInt(), nonceIsLE)
            val hash = digest.digest(digest.digest(bytes))
            val providedHash = hex.parseHex(hashHex)
            val target = BigInteger(1, hex.parseHex(targetHex))
            val hashAsBigInteger = BigInteger(1, hash.reversedArray())

            if (!MessageDigest.isEqual(hash, providedHash)) {
                return ValidationResult(ValidationStatus.REJECTED, "Hash mismatch")
            }
            if (hashAsBigInteger > target) {
                return ValidationResult(ValidationStatus.REJECTED, "Hash above target")
            }

            return ValidationResult(ValidationStatus.ACCEPTED)
        }

        private fun putNonce(buffer: ByteArray, offset: Int, nonce: Int, littleEndian: Boolean) {
            if (littleEndian) {
                buffer[offset] = (nonce and 0xFF).toByte()
                buffer[offset + 1] = ((nonce shr 8) and 0xFF).toByte()
                buffer[offset + 2] = ((nonce shr 16) and 0xFF).toByte()
                buffer[offset + 3] = ((nonce shr 24) and 0xFF).toByte()
            } else {
                buffer[offset] = ((nonce shr 24) and 0xFF).toByte()
                buffer[offset + 1] = ((nonce shr 16) and 0xFF).toByte()
                buffer[offset + 2] = ((nonce shr 8) and 0xFF).toByte()
                buffer[offset + 3] = (nonce and 0xFF).toByte()
            }
        }

        companion object {
            const val PLUGIN_ID: String = "pow-test-sha256"
        }
    }
}

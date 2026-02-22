package ru.itmo.enterprise.pow

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.itmo.enterprise.pow.model.ValidateRequest
import java.security.MessageDigest
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChallengeIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should complete full challenge flow`() {
        // 1. Get Challenge
        val challengeResult = mockMvc.perform(get("/challenge"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andReturn()

        val html = challengeResult.response.contentAsString
        val nonce = extractFromHtml(html, "nonce")
        val difficultyStr = extractFromHtml(html, "difficulty")
        val difficulty = difficultyStr?.toInt() ?: 20

        assertNotNull(nonce)
        assertTrue(difficulty > 0)

        // 2. Solve Challenge
        val (solution, hashHex) = solve(nonce!!, difficulty)

        // 3. Validate Solution
        val validateRequest = ValidateRequest(nonce, solution, hashHex)
        mockMvc.perform(
            post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should reject invalid solution`() {
        val validateRequest = ValidateRequest("invalid-nonce", "invalid-solution", "invalid-hash")
        mockMvc.perform(
            post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest))
        )
            .andExpect(status().isTooManyRequests)
    }

    private fun extractFromHtml(html: String, key: String): String? {
        val regex = """$key:\s*"?(.*?)"?[,}\n]""".toRegex()
        return regex.find(html)?.groupValues?.get(1)
    }

    private fun solve(nonce: String, difficulty: Int): Pair<String, String> {
        var solution = 0
        val digest = MessageDigest.getInstance("SHA-256")
        val hexFormat = HexFormat.of()

        while (true) {
            val solutionStr = solution.toString()
            val hash = digest.digest((nonce + solutionStr).toByteArray())
            if (leadingZeroBits(hash) >= difficulty) {
                return solutionStr to hexFormat.formatHex(hash)
            }
            solution++
        }
    }

    private fun leadingZeroBits(bytes: ByteArray): Int {
        var count = 0
        for (byte in bytes) {
            if (byte == 0.toByte()) {
                count += 8
            } else {
                count += Integer.numberOfLeadingZeros(byte.toInt() and 0xFF) - 24
                break
            }
        }
        return count
    }
}

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.itmo.enterprise.pow.model.*
import java.security.MessageDigest
import java.util.HexFormat

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NewChallengeIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should complete full challenge flow with new abstraction`() {
        // 1. Get Challenge
        val challengeResult = mockMvc.perform(get("/challenge"))
            .andExpect(status().isOk)
            .andReturn()

        val taskJson = challengeResult.response.contentAsString
        val task = objectMapper.readValue(taskJson, Task::class.java)
        
        assertEquals(JobType.POW_TEST_SHA256, task.jobType)
        val payload = task.payload as Sha256PowTaskPayload

        // 2. Solve Challenge
        val resultPayload = solve(payload)

        // 3. Validate Solution
        val resultMessage = ResultMessage(
            jobId = task.jobId,
            jobType = task.jobType,
            payload = resultPayload
        )

        mockMvc.perform(
            post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resultMessage))
        )
            .andExpect(status().isOk)
    }

    private fun solve(payload: Sha256PowTaskPayload): Sha256PowResultPayload {
        val hex = HexFormat.of()
        val data = hex.parseHex(payload.dataHex)
        val targetVal = java.math.BigInteger(1, hex.parseHex(payload.targetHex))
        val digest = MessageDigest.getInstance("SHA-256")

        for (nonce in payload.nonceRange.start until payload.nonceRange.end) {
            val currentData = data.copyOf()
            if (payload.nonceIsLE) {
                currentData[payload.nonceOffset] = (nonce and 0xFF).toByte()
                currentData[payload.nonceOffset + 1] = ((nonce shr 8) and 0xFF).toByte()
                currentData[payload.nonceOffset + 2] = ((nonce shr 16) and 0xFF).toByte()
                currentData[payload.nonceOffset + 3] = ((nonce shr 24) and 0xFF).toByte()
            } else {
                currentData[payload.nonceOffset] = ((nonce shr 24) and 0xFF).toByte()
                currentData[payload.nonceOffset + 1] = ((nonce shr 16) and 0xFF).toByte()
                currentData[payload.nonceOffset + 2] = ((nonce shr 8) and 0xFF).toByte()
                currentData[payload.nonceOffset + 3] = (nonce and 0xFF).toByte()
            }

            val hash1 = digest.digest(currentData)
            val hash2 = digest.digest(hash1)
            val reversedHash = hash2.reversedArray()
            val hashVal = java.math.BigInteger(1, reversedHash)

            if (hashVal <= targetVal) {
                return Sha256PowResultPayload(
                    dataHex = payload.dataHex,
                    nonce = nonce,
                    hashHex = hex.formatHex(hash2),
                    durationMs = 0,
                    attempts = nonce - payload.nonceRange.start + 1
                )
            }
        }
        throw IllegalStateException("No solution found in range")
    }
}

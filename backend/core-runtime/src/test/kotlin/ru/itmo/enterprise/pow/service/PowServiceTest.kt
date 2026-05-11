package ru.itmo.enterprise.pow.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.itmo.enterprise.pow.config.PowProperties
import java.security.MessageDigest
import java.util.HexFormat

class PowServiceTest {

    private val props = PowProperties(
        difficultyBits = 10,
        ttlSeconds = 60,
        hmacSecret = "secret"
    )
    private val powService = PowService(props)

    @Test
    fun `should verify correct pow`() {
        val nonce = "test-nonce"
        var solution = 0
        var found = false
        var solutionStr = ""
        var hashHex = ""

        while (!found) {
            solutionStr = solution.toString()
            val hash = MessageDigest.getInstance("SHA-256").digest((nonce + solutionStr).toByteArray())
            if (leadingZeroBits(hash) >= props.difficultyBits) {
                hashHex = HexFormat.of().formatHex(hash)
                found = true
            } else {
                solution++
            }
        }

        assertTrue(powService.verify(nonce, solutionStr, hashHex))
    }

    @Test
    fun `should reject incorrect solution`() {
        val nonce = "test-nonce"
        val solution = "123"
        val hash = MessageDigest.getInstance("SHA-256").digest((nonce + solution).toByteArray())
        val hashHex = HexFormat.of().formatHex(hash)

        if (leadingZeroBits(hash) < props.difficultyBits) {
            assertFalse(powService.verify(nonce, solution, hashHex))
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

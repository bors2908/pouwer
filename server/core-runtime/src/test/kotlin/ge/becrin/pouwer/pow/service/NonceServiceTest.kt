package ge.becrin.pouwer.pow.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ge.becrin.pouwer.pow.config.PowProperties
import java.util.Base64

class NonceServiceTest {

    private val props = PowProperties(
        difficultyBits = 10,
        ttlSeconds = 60,
        hmacSecret = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    )
    private val nonceService = NonceService(props)

    @Test
    fun `should generate and validate nonce`() {
        val nonce = nonceService.generateNonce()
        assertNotNull(nonce)

        val payload = nonceService.parseAndValidateNonce(nonce)
        assertNotNull(payload)
        assertEquals(16, payload!!.random.size)
        assertTrue(payload.expiresAt > System.currentTimeMillis())
    }

    @Test
    fun `should fail on tampered nonce`() {
        val nonce = nonceService.generateNonce()
        val decoded = Base64.getUrlDecoder().decode(nonce)
        decoded[0] = (decoded[0].toInt() xor 0xFF).toByte()
        val tamperedNonce = Base64.getUrlEncoder().withoutPadding().encodeToString(decoded)

        val payload = nonceService.parseAndValidateNonce(tamperedNonce)
        assertNull(payload)
    }

    @Test
    fun `should fail on expired nonce`() {
        val expiredProps = props.copy(ttlSeconds = -10)
        val expiredService = NonceService(expiredProps)
        val nonce = expiredService.generateNonce()

        val payload = expiredService.parseAndValidateNonce(nonce)
        assertNull(payload)
    }
}

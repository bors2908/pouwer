package ru.itmo.enterprise.pow.service

import org.springframework.stereotype.Service
import ru.itmo.enterprise.pow.config.PowProperties
import ru.itmo.enterprise.pow.model.ChallengePayload
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class NonceService(
    private val props: PowProperties
) {
    private val secureRandom = SecureRandom()
    private val algorithm = "HmacSHA256"

    fun generateNonce(): String {
        val randomBytes = ByteArray(16)
        secureRandom.nextBytes(randomBytes)
        val expiresAt = System.currentTimeMillis() + props.ttlSeconds * 1000

        val payloadBytes = ByteBuffer.allocate(randomBytes.size + 8)
            .put(randomBytes)
            .putLong(expiresAt)
            .array()

        val signature = computeHmac(payloadBytes)
        val finalNonceBytes = ByteArray(payloadBytes.size + signature.size)
        System.arraycopy(payloadBytes, 0, finalNonceBytes, 0, payloadBytes.size)
        System.arraycopy(signature, 0, finalNonceBytes, payloadBytes.size, signature.size)

        return Base64.getUrlEncoder().withoutPadding().encodeToString(finalNonceBytes)
    }

    fun parseAndValidateNonce(nonce: String): ChallengePayload? {
        return try {
            val decodedBytes = Base64.getUrlDecoder().decode(nonce)
            if (decodedBytes.size < 32 + 8 + 16) { // signature(32) + long(8) + min_random(16)
                return null
            }

            val payloadSize = decodedBytes.size - 32
            val payloadBytes = decodedBytes.sliceArray(0 until payloadSize)
            val providedSignature = decodedBytes.sliceArray(payloadSize until decodedBytes.size)

            val expectedSignature = computeHmac(payloadBytes)
            if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
                return null
            }

            val randomBytes = payloadBytes.sliceArray(0 until payloadBytes.size - 8)
            val expiresAt = ByteBuffer.wrap(payloadBytes, payloadBytes.size - 8, 8).long

            if (System.currentTimeMillis() > expiresAt) {
                return null
            }

            ChallengePayload(randomBytes, expiresAt)
        } catch (e: Exception) {
            null
        }
    }

    private fun computeHmac(data: ByteArray): ByteArray {
        val mac = Mac.getInstance(algorithm)
        val secretKey = SecretKeySpec(Base64.getDecoder().decode(props.hmacSecret), algorithm)
        mac.init(secretKey)
        return mac.doFinal(data)
    }
}

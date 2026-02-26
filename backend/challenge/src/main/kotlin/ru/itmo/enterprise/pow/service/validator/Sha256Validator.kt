package ru.itmo.enterprise.pow.service.validator

import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.model.Sha256PowResultPayload
import ru.itmo.enterprise.pow.model.Sha256PowTaskPayload
import ru.itmo.enterprise.pow.service.PowValidator
import java.security.MessageDigest
import java.util.HexFormat

@Component
class Sha256Validator : PowValidator<Sha256PowTaskPayload, Sha256PowResultPayload> {
    private val hex = HexFormat.of()

    override fun verify(
        taskPayload: Sha256PowTaskPayload,
        resultPayload: Sha256PowResultPayload
    ): Boolean {
        return try {
            val data = hex.parseHex(taskPayload.dataHex)
            if (taskPayload.nonceOffset < 0 || taskPayload.nonceOffset + 4 > data.size) {
                return false
            }

            // Insert nonce with endianness
            val nonce = resultPayload.nonce
            if (taskPayload.nonceIsLE) {
                data[taskPayload.nonceOffset] = (nonce and 0xFF).toByte()
                data[taskPayload.nonceOffset + 1] = ((nonce shr 8) and 0xFF).toByte()
                data[taskPayload.nonceOffset + 2] = ((nonce shr 16) and 0xFF).toByte()
                data[taskPayload.nonceOffset + 3] = ((nonce shr 24) and 0xFF).toByte()
            } else {
                data[taskPayload.nonceOffset] = ((nonce shr 24) and 0xFF).toByte()
                data[taskPayload.nonceOffset + 1] = ((nonce shr 16) and 0xFF).toByte()
                data[taskPayload.nonceOffset + 2] = ((nonce shr 8) and 0xFF).toByte()
                data[taskPayload.nonceOffset + 3] = (nonce and 0xFF).toByte()
            }

            // Compute double SHA256
            val digest = MessageDigest.getInstance("SHA-256")
            val hash1 = digest.digest(data)
            val hash2 = digest.digest(hash1)

            // Bitcoin comparison: result of double-SHA256 is treated as LE uint256.
            // Target is usually also provided in a way that BigInteger(1, targetBytes) works.
            // However, Bitcoin's hash comparison is often shown as hash[::-1] <= target.
            // We'll stick to a simple byte comparison or BigInteger(1, reversedHash) if it's Bitcoin.
            // For general POW_TEST_SHA256, let's just use BigInteger(1, hash2).
            
            // To be safe and support both, we should probably follow what Bitcoin does exactly:
            // The hash is reversed before being treated as a large integer.
            val reversedHash = hash2.reversedArray()
            val hashVal = java.math.BigInteger(1, reversedHash)
            val targetVal = java.math.BigInteger(1, hex.parseHex(taskPayload.targetHex))

            hashVal.compareTo(targetVal) <= 0
        } catch (e: Exception) {
            false
        }
    }
}

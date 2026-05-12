package ge.becrin.pouwer.pow.service

import org.springframework.stereotype.Service
import ge.becrin.pouwer.pow.config.PowProperties
import java.security.MessageDigest
import java.util.HexFormat

@Service
class PowService(
    private val props: PowProperties
) {
    fun verify(nonce: String, solution: String, clientHashHex: String): Boolean {
        return try {
            val clientHash = HexFormat.of().parseHex(clientHashHex)
            val input = (nonce + solution).toByteArray(Charsets.UTF_8)
            val serverHash = sha256(input)

            if (!MessageDigest.isEqual(clientHash, serverHash)) {
                return false
            }

            leadingZeroBits(serverHash) >= props.difficultyBits
        } catch (e: Exception) {
            false
        }
    }

    private fun sha256(input: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input)
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

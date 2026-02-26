package ru.itmo.enterprise.pow.service.validator

import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.client.monero.stratum.StratumSubmitService
import ru.itmo.enterprise.pow.model.MoneroRandomXResultPayload
import ru.itmo.enterprise.pow.model.MoneroRandomXTaskPayload
import ru.itmo.enterprise.pow.service.PowValidator
import java.math.BigInteger
import java.util.HexFormat

@Component
class MoneroRandomXValidator(
    val stratumSubmitService: StratumSubmitService
) : PowValidator<MoneroRandomXTaskPayload, MoneroRandomXResultPayload> {
    private val hex = HexFormat.of()

    override fun verify(
        taskPayload: MoneroRandomXTaskPayload,
        resultPayload: MoneroRandomXResultPayload
    ): Boolean {
        try {
            // Basic sanity: nonce must fit uint32 (Monero uses 32-bit nonce in blockhashing_blob)
            val nonce = resultPayload.nonce
            if (nonce < 0L || nonce > 0xFFFF_FFFFL) return false

            // Hash must be present and valid hex
            val hashBytes = try {
                hex.parseHex(resultPayload.hash)
            } catch (ex: Exception) {
                return false
            }

            // Convert hash bytes (as submitted by worker) into a bigint interpreting bytes as little-endian
            val hashBigInt = u8ToBigIntLE(hashBytes)

            val target = resolveTarget(taskPayload) ?: return false

            // Fast numeric check first
            if (hashBigInt > target) {
                return false
            }

            // Now authoritative check via daemon
            return stratumSubmitService.submitShare(taskPayload.stratumJobId, nonce)
        } catch (ex: Exception) {
            // Defensive: any unexpected error -> invalid
            return false
        }
    }

    private fun resolveTarget(task: MoneroRandomXTaskPayload): BigInteger? {
        return when {
            !task.targetHex.isNullOrBlank() -> {
                try {
                    BigInteger(task.targetHex, 16)
                } catch (ex: Exception) {
                    null
                }
            }

            !task.difficulty.isNullOrBlank() -> {
                val difficulty = try {
                    BigInteger(task.difficulty)
                } catch (ex: Exception) {
                    return null
                }
                if (difficulty <= BigInteger.ZERO) return null
                val max = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)
                max.divide(difficulty)
            }

            else -> null
        }
    }

    // Interpret byte array as little-endian unsigned integer -> BigInteger
    private fun u8ToBigIntLE(bytes: ByteArray): BigInteger {
        if (bytes.isEmpty()) return BigInteger.ZERO
        return BigInteger(1, bytes.reversedArray())
    }
}

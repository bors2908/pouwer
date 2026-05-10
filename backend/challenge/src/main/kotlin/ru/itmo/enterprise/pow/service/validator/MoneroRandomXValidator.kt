package ru.itmo.enterprise.pow.service.validator

import org.slf4j.LoggerFactory
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
            if (nonce < 0L || nonce > 0xFFFF_FFFFL) {
                log.warn("Invalid nonce outside of the boundaries: $nonce")

                return false
            }

            val hash = resultPayload.hash

            // Hash must be present and valid hex
            val hashBytes = try {
                hex.parseHex(hash)
            } catch (ex: Exception) {
                log.warn("Could not parse hex hash: $hash", ex)

                return false
            }

            // Convert hash bytes (as submitted by worker) into a bigint interpreting bytes as little-endian
            val hashBigInt = u8ToBigIntLE(hashBytes)

            val target = resolveTarget(taskPayload) ?: return false

            // Fast numeric check first
            if (hashBigInt > target) {
                log.warn("Hash $hashBigInt is greater than target $target")

                //return false
            }

            // Now authoritative check via daemon
            return stratumSubmitService.submitShare(taskPayload.stratumJobId, nonce, hash)
        } catch (ex: Exception) {
            log.warn("Could not process checks", ex)

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

            else -> null
        }
    }

    // Interpret byte array as little-endian unsigned integer -> BigInteger
    private fun u8ToBigIntLE(bytes: ByteArray): BigInteger {
        if (bytes.isEmpty()) return BigInteger.ZERO
        return BigInteger(1, bytes.reversedArray())
    }

    companion object {
        private val log = LoggerFactory.getLogger(MoneroRandomXValidator::class.java)
    }
}

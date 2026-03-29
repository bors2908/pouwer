// StratumToMoneroConverter.kt
package ru.itmo.enterprise.pow.client.monero.stratum

import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.model.MoneroRandomXTaskPayload
import java.math.BigInteger
import java.util.UUID

@Component
class StratumToMoneroConverter {

    /**
     * Convert RawStratumJob -> MoneroRandomXTaskPayload.
     * This is separate from storage; it computes difficulty if pool sent target.
     */
    fun convert(raw: RawStratumJob): MoneroRandomXTaskPayload {
        val target = raw.targetHex
        val difficultyStr: String? = when {
            target != null -> {
                try {
                    val t = BigInteger(target, 16)
                    if (t == BigInteger.ZERO) null
                    else {
                        val max = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)
                        max.divide(t).toString()
                    }
                } catch (_: Exception) {
                    null
                }
            }

            else -> null
        }

        return MoneroRandomXTaskPayload(
            id = UUID.randomUUID().toString(),
            blob = raw.blob,
            targetHex = raw.targetHex,
            stratumJobId = raw.jobId,
            seedHash = raw.seedHash,
        )
    }
}

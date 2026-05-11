package ru.itmo.enterprise.challenge.payload.monero

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class StratumToMoneroConverter {
    fun convert(raw: RawStratumJob): MoneroRandomXTaskPayload {
        return MoneroRandomXTaskPayload(
            id = UUID.randomUUID().toString(),
            blob = raw.blob,
            targetHex = raw.targetHex,
            height = raw.height,
            stratumJobId = raw.jobId,
            seedHash = raw.seedHash
        )
    }
}

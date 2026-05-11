package ru.itmo.enterprise.challenge.payload.monero

import java.util.UUID

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

package ru.itmo.enterprise.pow.client.monero.stratum

data class RawStratumJob(
    val jobId: String,
    val blob: String,           // blockhashing_blob hex (no 0x)
    val targetHex: String?,     // share target hex (big-endian, no 0x)
    val seedHash: String?,      // optional RandomX seed hash
    val height: Long?,          // optional
)

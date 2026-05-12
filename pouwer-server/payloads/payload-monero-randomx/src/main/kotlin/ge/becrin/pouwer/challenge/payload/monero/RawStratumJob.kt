package ge.becrin.pouwer.challenge.payload.monero

data class RawStratumJob(
    val jobId: String,
    val blob: String,
    val targetHex: String?,
    val seedHash: String?,
    val height: Long?
)

package ge.becrin.pouwer.challenge.payload.monero

data class MoneroRandomXTaskPayload(
    val id: String,
    val blob: String,
    val targetHex: String?,
    val height: Long?,
    val stratumJobId: String? = null,
    val seedHash: String? = null
)

data class MoneroRandomXResultPayload(
    val taskId: String,
    val nonce: Long,
    val hash: String
)

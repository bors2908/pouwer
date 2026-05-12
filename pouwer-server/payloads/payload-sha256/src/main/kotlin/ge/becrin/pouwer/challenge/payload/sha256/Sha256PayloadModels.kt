package ge.becrin.pouwer.challenge.payload.sha256

import ge.becrin.pouwer.challenge.api.NonceRange

data class Sha256PowTaskPayload(
    val dataHex: String,
    val nonceOffset: Int,
    val nonceIsLE: Boolean,
    val targetHex: String,
    val nonceRange: NonceRange
)

data class Sha256PowResultPayload(
    val nonce: Long,
    val hashHex: String
)

package ge.becrin.pouwer.challenge.payload.bitcoin

import ge.becrin.pouwer.challenge.api.NonceRange

data class BitcoinSha256TaskPayload(
    val dataHex: String,
    val nonceOffset: Int,
    val nonceIsLE: Boolean,
    val targetHex: String,
    val nonceRange: NonceRange
)

data class BitcoinSha256ResultPayload(
    val nonce: Long,
    val hashHex: String
)

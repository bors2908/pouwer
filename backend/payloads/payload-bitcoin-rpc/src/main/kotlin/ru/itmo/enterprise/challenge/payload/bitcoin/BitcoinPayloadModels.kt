package ru.itmo.enterprise.challenge.payload.bitcoin

import ru.itmo.enterprise.challenge.api.NonceRange

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

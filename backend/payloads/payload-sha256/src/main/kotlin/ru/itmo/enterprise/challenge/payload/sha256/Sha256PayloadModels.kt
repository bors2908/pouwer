package ru.itmo.enterprise.challenge.payload.sha256

import ru.itmo.enterprise.challenge.api.NonceRange

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

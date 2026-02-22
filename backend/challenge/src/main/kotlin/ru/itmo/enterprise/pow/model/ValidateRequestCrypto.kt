package ru.itmo.enterprise.pow.model

data class ValidateRequestCrypto(
    val jobId: String,
    val nonce: Long,
    val hashHex: String
)

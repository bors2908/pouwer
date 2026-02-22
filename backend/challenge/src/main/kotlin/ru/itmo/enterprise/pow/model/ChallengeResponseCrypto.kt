package ru.itmo.enterprise.pow.model

data class ChallengeResponseCrypto(
    val jobId: String,
    val headerPrefixHex: String,
    val targetHex: String,
    val nonceStart: Long,
    val nonceEnd: Long
)

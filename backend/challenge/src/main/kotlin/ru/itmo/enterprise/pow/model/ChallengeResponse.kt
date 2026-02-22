package ru.itmo.enterprise.pow.model

data class ChallengeResponse(
    val nonce: String,
    val difficulty: Int,
    val expiresAt: Long? = null
)

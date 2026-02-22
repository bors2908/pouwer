package ru.itmo.enterprise.pow.model

data class ValidateRequest(
    val nonce: String,
    val solution: String,
    val hash: String
)

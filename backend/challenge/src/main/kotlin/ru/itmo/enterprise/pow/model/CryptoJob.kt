package ru.itmo.enterprise.pow.model

data class CryptoJob(
    val jobId: String,
    val headerPrefixHex: String,
    val targetHex: String,
    val blockTemplate: Map<String, Any>, // Store raw result for reconstruction
    val createdAt: Long = System.currentTimeMillis(),
    var solved: Boolean = false
)

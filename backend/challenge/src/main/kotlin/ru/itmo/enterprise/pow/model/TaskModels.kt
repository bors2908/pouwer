package ru.itmo.enterprise.pow.model

import java.util.UUID

data class Task(
    val jobId: UUID,
    val jobType: JobType,
    val expiresAt: Long,
    val payload: TaskPayload,
    val leaseHmac: String? = null
)

data class ResultMessage(
    val jobId: UUID,
    val payload: ResultPayload,
    val meta: Map<String, String>? = emptyMap(),
    val leaseHmac: String? = null,
    val durationMs: Long?,
    val attempts: Long?
)

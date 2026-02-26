package ru.itmo.enterprise.pow.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "jobType"
)
@JsonSubTypes(
    JsonSubTypes.Type(
        value = Sha256PowTaskPayload::class,
        name = "POW_TEST_SHA256"
    ),
    JsonSubTypes.Type(
        value = Sha256PowTaskPayload::class,
        name = "BITCOIN_RPC_SHA256"
    ),
    JsonSubTypes.Type(
        value = MoneroRandomXTaskPayload::class,
        name = "RANDOMX_MONERO"
    )
)
sealed interface TaskPayload

data class Sha256PowTaskPayload(
    val dataHex: String,
    val nonceOffset: Int,
    val nonceIsLE: Boolean,
    val targetHex: String,
    val nonceRange: NonceRange
) : TaskPayload

data class MoneroRandomXTaskPayload(
    val id: String,
    val blob: String,
    val difficulty: String?,
    val targetHex: String?
) : TaskPayload

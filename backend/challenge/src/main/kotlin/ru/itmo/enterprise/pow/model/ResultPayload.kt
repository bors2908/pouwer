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
        value = Sha256PowResultPayload::class,
        name = "POW_TEST_SHA256"
    ),
    JsonSubTypes.Type(
        value = Sha256PowResultPayload::class,
        name = "BITCOIN_RPC_SHA256"
    ),
    JsonSubTypes.Type(
        value = MoneroRandomXResultPayload::class,
        name = "MONERO_RANDOMX"
    )
)
sealed interface ResultPayload

data class Sha256PowResultPayload(
    val dataHex: String,
    val nonce: Long,
    val hashHex: String
) : ResultPayload

data class MoneroRandomXResultPayload(
    val taskId: String,
    val nonce: Long,
    val hash: String
) : ResultPayload

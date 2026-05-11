package ru.itmo.enterprise.challenge.api

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

const val CHALLENGE_PLUGIN_CONTRACT_VERSION: String = "1.0"

data class Task(
    val jobId: UUID,
    val pluginId: String,
    val expiresAt: Long,
    val payload: JsonNode,
    val leaseHmac: String? = null
)

data class ResultMessage(
    val jobId: UUID,
    val pluginId: String? = null,
    val payload: JsonNode,
    val meta: Map<String, String>? = emptyMap(),
    val leaseHmac: String? = null,
    val durationMs: Long?,
    val attempts: Long?
)

data class ValidationResult(
    val status: ValidationStatus,
    val reason: String? = null
)

data class PayloadSupportContext(
    val requestedPluginId: String? = null,
    val legacyJobType: String? = null,
    val workerId: String? = null
)

data class PayloadBuildRequest(
    val workerId: String?,
    val nowMillis: Long,
    val taskTtlMillis: Long,
    val requestedPluginId: String? = null,
    val legacyJobType: String? = null
)

enum class ValidationStatus {
    ACCEPTED,
    REJECTED,
    CONFLICT
}

interface PayloadPlugin {
    fun id(): String

    fun version(): String

    val contractVersion: String
        get() = CHALLENGE_PLUGIN_CONTRACT_VERSION

    fun supports(context: PayloadSupportContext): Boolean

    fun buildPayload(request: PayloadBuildRequest): Task

    fun validateResult(task: Task, result: ResultMessage): ValidationResult
}

class DuplicatePluginIdException(pluginId: String) :
    IllegalStateException("Duplicate plugin id: $pluginId")

class ContractMismatchException(pluginId: String, expected: String, actual: String) :
    IllegalStateException("Plugin $pluginId contract mismatch. Expected $expected, got $actual")

class UnsupportedPluginException(pluginId: String, available: Set<String>) :
    IllegalArgumentException("Unsupported plugin: $pluginId. Available plugins: $available")

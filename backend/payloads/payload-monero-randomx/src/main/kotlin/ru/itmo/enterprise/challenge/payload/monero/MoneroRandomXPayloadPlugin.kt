package ru.itmo.enterprise.challenge.payload.monero

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ru.itmo.enterprise.challenge.api.ResultMessage
import ru.itmo.enterprise.challenge.api.Task
import ru.itmo.enterprise.challenge.api.ValidationResult
import ru.itmo.enterprise.challenge.api.ValidationStatus
import java.math.BigInteger
import java.util.HexFormat
import java.util.UUID

class MoneroRandomXPayloadPlugin(
    private val jobStore: StratumJobStore = StratumJobStore(),
    private val converter: StratumToMoneroConverter = StratumToMoneroConverter(),
    private val stratumSubmitService: StratumSubmitService = StratumSubmitService()
) {
    private val mapper: ObjectMapper = jacksonObjectMapper()
    private val hex = HexFormat.of()

    val pluginId: String = PLUGIN_ID

    fun createTask(workerId: String?, nowMillis: Long, taskTtlMillis: Long): Task {
        val rawJob = jobStore.getLatest()
            ?: throw IllegalStateException("No stratum job available")

        val payload = converter.convert(rawJob)

        return Task(
            jobId = UUID.randomUUID(),
            pluginId = pluginId,
            expiresAt = nowMillis + taskTtlMillis,
            payload = mapper.valueToTree(payload)
        )
    }

    fun validate(task: Task, result: ResultMessage): ValidationResult {
        if (task.pluginId != pluginId) {
            return ValidationResult(ValidationStatus.REJECTED, "Plugin mismatch")
        }

        val taskPayload = runCatching { mapper.treeToValue(task.payload, MoneroRandomXTaskPayload::class.java) }
            .getOrElse { return ValidationResult(ValidationStatus.REJECTED, "Invalid task payload") }
        val resultPayload = runCatching { mapper.treeToValue(result.payload, MoneroRandomXResultPayload::class.java) }
            .getOrElse { return ValidationResult(ValidationStatus.REJECTED, "Invalid result payload") }

        if (resultPayload.nonce < 0L || resultPayload.nonce > 0xFFFF_FFFFL) {
            return ValidationResult(ValidationStatus.REJECTED, "Invalid nonce")
        }

        val hashBytes = runCatching { hex.parseHex(resultPayload.hash) }
            .getOrElse { return ValidationResult(ValidationStatus.REJECTED, "Invalid hash") }

        val target = resolveTarget(taskPayload)
            ?: return ValidationResult(ValidationStatus.REJECTED, "Missing target")

        val hashBigInt = BigInteger(1, hashBytes.reversedArray())
        if (hashBigInt > target) {
            return ValidationResult(ValidationStatus.REJECTED, "Hash above target")
        }

        return if (stratumSubmitService.submitShare(taskPayload.stratumJobId, resultPayload.nonce, resultPayload.hash)) {
            ValidationResult(ValidationStatus.ACCEPTED)
        } else {
            ValidationResult(ValidationStatus.REJECTED, "Pool rejected share")
        }
    }

    private fun resolveTarget(task: MoneroRandomXTaskPayload): BigInteger? {
        val targetHex = task.targetHex ?: return null
        return runCatching { BigInteger(targetHex, 16) }.getOrNull()
    }

    companion object {
        const val PLUGIN_ID: String = "monero-randomx"
    }
}

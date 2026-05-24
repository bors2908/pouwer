package ge.becrin.pouwer.challenge.payload.monero

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import ge.becrin.pouwer.plugin.base.PluginPayloadService
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.util.HexFormat
import java.util.UUID

@Component
class MoneroRandomXPayloadPlugin(
    private val jobStore: StratumJobStore,
    private val converter: StratumToMoneroConverter,
    private val stratumSubmitService: ShareSubmitter
) : PluginPayloadService {
    private val mapper: ObjectMapper = jacksonObjectMapper()
    private val hex = HexFormat.of()

    override val pluginId: String = PLUGIN_ID

    override fun createTask(workerId: String?, nowMillis: Long, taskTtlMillis: Long): Task {
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

    override fun validate(task: Task, result: ResultMessage): ValidationResult {
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

package ge.becrin.pouwer.challenge.payload.sha256

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import ge.becrin.pouwer.challenge.api.NonceRange
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import ge.becrin.pouwer.plugin.base.PluginPayloadService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID
import kotlin.random.Random

@Component
class Sha256PayloadPlugin(
    @param:Value($$"${plugin.sha256.target:0000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff}")
    private val targetHex: String,
    @param:Value($$"${plugin.sha256.nonce-range-size:1000000}")
    private val nonceRangeSize: Long
) : PluginPayloadService {
    private val hex = HexFormat.of()
    private val mapper: ObjectMapper = jacksonObjectMapper()
    override val pluginId: String = PLUGIN_ID

    override fun createTask(workerId: String?, nowMillis: Long, taskTtlMillis: Long): Task {
        val data = ByteArray(32)
        Random.nextBytes(data)

        val payload = Sha256PowTaskPayload(
            dataHex = hex.formatHex(data),
            nonceOffset = 0,
            nonceIsLE = false,
            targetHex = targetHex,
            nonceRange = NonceRange(0, nonceRangeSize)
        )

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

        val taskPayload = runCatching { mapper.treeToValue(task.payload, Sha256PowTaskPayload::class.java) }
            .getOrElse { return ValidationResult(ValidationStatus.REJECTED, "Invalid task payload") }
        val resultPayload = runCatching { mapper.treeToValue(result.payload, Sha256PowResultPayload::class.java) }
            .getOrElse { return ValidationResult(ValidationStatus.REJECTED, "Invalid result payload") }

        if (resultPayload.nonce < taskPayload.nonceRange.start || resultPayload.nonce >= taskPayload.nonceRange.end) {
            return ValidationResult(ValidationStatus.CONFLICT, "Nonce outside lease")
        }

        return if (verifyHash(taskPayload, resultPayload)) {
            ValidationResult(ValidationStatus.ACCEPTED)
        } else {
            ValidationResult(ValidationStatus.REJECTED, "Hash invalid")
        }
    }

    private fun verifyHash(taskPayload: Sha256PowTaskPayload, resultPayload: Sha256PowResultPayload): Boolean {
        return try {
            val data = hex.parseHex(taskPayload.dataHex)
            if (taskPayload.nonceOffset < 0 || taskPayload.nonceOffset + 4 > data.size) {
                return false
            }

            val nonce = resultPayload.nonce
            if (taskPayload.nonceIsLE) {
                data[taskPayload.nonceOffset] = (nonce and 0xFF).toByte()
                data[taskPayload.nonceOffset + 1] = ((nonce shr 8) and 0xFF).toByte()
                data[taskPayload.nonceOffset + 2] = ((nonce shr 16) and 0xFF).toByte()
                data[taskPayload.nonceOffset + 3] = ((nonce shr 24) and 0xFF).toByte()
            } else {
                data[taskPayload.nonceOffset] = ((nonce shr 24) and 0xFF).toByte()
                data[taskPayload.nonceOffset + 1] = ((nonce shr 16) and 0xFF).toByte()
                data[taskPayload.nonceOffset + 2] = ((nonce shr 8) and 0xFF).toByte()
                data[taskPayload.nonceOffset + 3] = (nonce and 0xFF).toByte()
            }

            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(digest.digest(data))
            val reversedHash = hash.reversedArray()

            val hashVal = BigInteger(1, reversedHash)
            val targetVal = BigInteger(1, hex.parseHex(taskPayload.targetHex))
            val providedHash = hex.parseHex(resultPayload.hashHex)

            MessageDigest.isEqual(providedHash, hash) && hashVal <= targetVal
        } catch (ex: Exception) {
            false
        }
    }

    companion object {
        const val PLUGIN_ID: String = "pow-test-sha256"
    }
}

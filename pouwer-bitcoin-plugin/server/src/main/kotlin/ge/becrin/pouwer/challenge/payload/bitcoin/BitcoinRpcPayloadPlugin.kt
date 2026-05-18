package ge.becrin.pouwer.challenge.payload.bitcoin

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
import ge.becrin.pouwer.plugin.base.PluginPayloadService
import org.bitcoinj.core.Utils
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

@Component
class BitcoinRpcPayloadPlugin(
    private val rpcClient: BitcoinRpcClient,
    private val templateStore: BitcoinTemplateStore,
    private val blockBuilder: BitcoinBlockBuilder
) : PluginPayloadService {
    private val mapper: ObjectMapper = jacksonObjectMapper()
    private val hex = HexFormat.of()
    private val chunkSize = 500_000L

    override val pluginId: String = PLUGIN_ID

    override fun createTask(workerId: String?, nowMillis: Long, taskTtlMillis: Long): Task {
        val template = rpcClient.getBlockTemplate()
        val jobId = UUID.randomUUID()
        val expiresAt = nowMillis + taskTtlMillis
        val workerNum = workerId?.toLongOrNull() ?: 0L
        val nonceStart = workerNum * chunkSize
        val nonceEnd = nonceStart + chunkSize

        val payload = blockBuilder.createTaskPayload(template, nonceStart, nonceEnd)

        templateStore.save(jobId, template, expiresAt)
        return Task(
            jobId = jobId,
            pluginId = pluginId,
            expiresAt = expiresAt,
            payload = mapper.valueToTree(payload)
        )
    }

    override fun validate(task: Task, result: ResultMessage): ValidationResult {
        if (task.pluginId != pluginId) {
            return ValidationResult(ValidationStatus.REJECTED, "Plugin mismatch")
        }

        val taskPayload = runCatching { mapper.treeToValue(task.payload, BitcoinSha256TaskPayload::class.java) }
            .getOrElse { return ValidationResult(ValidationStatus.REJECTED, "Invalid task payload") }
        val resultPayload = runCatching { mapper.treeToValue(result.payload, BitcoinSha256ResultPayload::class.java) }
            .getOrElse { return ValidationResult(ValidationStatus.REJECTED, "Invalid result payload") }

        if (resultPayload.nonce < taskPayload.nonceRange.start || resultPayload.nonce >= taskPayload.nonceRange.end) {
            return ValidationResult(ValidationStatus.CONFLICT, "Nonce outside lease")
        }

        if (!verifyHash(taskPayload, resultPayload)) {
            return ValidationResult(ValidationStatus.REJECTED, "Hash invalid")
        }

        val template = templateStore.find(task.jobId)
            ?: return ValidationResult(ValidationStatus.CONFLICT, "Template lost")

        val header = ByteArray(80)
        System.arraycopy(hex.parseHex(taskPayload.dataHex), 0, header, 0, 76)
        Utils.uint32ToByteArrayLE(resultPayload.nonce, header, 76)

        val block = blockBuilder.reconstructBlock(template, header)
        val submitResult = rpcClient.submitBlock(hex.formatHex(block.bitcoinSerialize()))

        return if (submitResult == null) {
            ValidationResult(ValidationStatus.ACCEPTED)
        } else {
            ValidationResult(ValidationStatus.REJECTED, "RPC rejected block: $submitResult")
        }
    }

    private fun verifyHash(taskPayload: BitcoinSha256TaskPayload, resultPayload: BitcoinSha256ResultPayload): Boolean {
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
        const val PLUGIN_ID: String = "bitcoin-rpc-sha256"
    }
}

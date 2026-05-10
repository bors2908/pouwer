package ru.itmo.enterprise.pow.service.result

import org.bitcoinj.core.Utils
import org.springframework.stereotype.Service
import ru.itmo.enterprise.pow.client.BitcoinRpcClient
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.Sha256PowResultPayload
import ru.itmo.enterprise.pow.model.Sha256PowTaskPayload
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.model.ValidationResult
import ru.itmo.enterprise.pow.service.LeaseManager
import ru.itmo.enterprise.pow.service.PowValidator
import ru.itmo.enterprise.pow.service.TaskStore
import ru.itmo.enterprise.pow.service.bitcoin.BitcoinBlockBuilder
import ru.itmo.enterprise.pow.service.bitcoin.BitcoinTemplateStore
import java.util.HexFormat

@Service
class BitcoinPowResultService(
    private val leaseManager: LeaseManager,
    taskStore: TaskStore,
    private val validator: PowValidator<Sha256PowTaskPayload, Sha256PowResultPayload>,
    private val rpcClient: BitcoinRpcClient,
    private val templateStore: BitcoinTemplateStore,
    private val blockBuilder: BitcoinBlockBuilder
) : BaseResultService<Sha256PowTaskPayload, Sha256PowResultPayload>(
    taskStore = taskStore,
    taskPayloadClass = Sha256PowTaskPayload::class,
    resultPayloadClass = Sha256PowResultPayload::class
) {
    override val type: JobType = JobType.BITCOIN_RPC_SHA256
    private val hex = HexFormat.of()

    override fun validate(
        task: Task,
        taskPayload: Sha256PowTaskPayload,
        resultPayload: Sha256PowResultPayload
    ): ValidationResult {
        if (!leaseManager.isNonceAllowed(task, resultPayload.nonce)) {
            return conflict("Nonce outside lease")
        }

        if (!validator.verify(taskPayload, resultPayload)) {
            return rejected("Hash invalid")
        }

        val template = templateStore.find(task.jobId)
            ?: return conflict("Template lost")

        val header = ByteArray(80)
        System.arraycopy(hex.parseHex(taskPayload.dataHex), 0, header, 0, 76)
        Utils.uint32ToByteArrayLE(resultPayload.nonce, header, 76)

        val block = blockBuilder.reconstructBlock(template, header)
        val submitResult = rpcClient.submitBlock(hex.formatHex(block.bitcoinSerialize()))
        return if (submitResult == null) {
            accepted()
        } else {
            rejected("RPC rejected block: $submitResult")
        }
    }
}

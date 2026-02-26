package ru.itmo.enterprise.pow.service.result

import org.bitcoinj.core.Block
import org.bitcoinj.core.Coin
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.Utils
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.springframework.stereotype.Service
import ru.itmo.enterprise.pow.client.BitcoinRpcClient
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.ResultMessage
import ru.itmo.enterprise.pow.model.Sha256PowResultPayload
import ru.itmo.enterprise.pow.model.Sha256PowTaskPayload
import ru.itmo.enterprise.pow.model.ValidationResult
import ru.itmo.enterprise.pow.model.ValidationStatus
import ru.itmo.enterprise.pow.service.LeaseManager
import ru.itmo.enterprise.pow.service.PowValidator
import ru.itmo.enterprise.pow.service.ResultService
import ru.itmo.enterprise.pow.service.TaskStore
import ru.itmo.enterprise.pow.service.orchestrator.BitcoinPowTaskOrchestrator
import java.util.HexFormat

@Service
class BitcoinPowResultService(
    private val taskStore: TaskStore,
    private val leaseManager: LeaseManager,
    private val validator: PowValidator<Sha256PowTaskPayload, Sha256PowResultPayload>,
    private val rpcClient: BitcoinRpcClient
) : ResultService {
    override val type: JobType = JobType.BITCOIN_RPC_SHA256
    private val params = RegTestParams.get()
    private val hex = HexFormat.of()

    override fun handleResult(result: ResultMessage): ValidationResult {
        val task = taskStore.find(result.jobId)
            ?: return ValidationResult(ValidationStatus.CONFLICT, "Unknown job")

        val resultPayload = result.payload as? Sha256PowResultPayload
            ?: return ValidationResult(ValidationStatus.REJECTED, "Invalid payload type")

        if (!leaseManager.isNonceAllowed(task, resultPayload.nonce))
            return ValidationResult(ValidationStatus.CONFLICT, "Nonce outside lease")

        val taskPayload = task.payload as? Sha256PowTaskPayload
            ?: return ValidationResult(ValidationStatus.REJECTED, "Invalid task payload type")

        val valid = validator.verify(taskPayload, resultPayload)
        if (!valid) return ValidationResult(ValidationStatus.REJECTED, "Hash invalid")

        // Reconstruct block and submit
        val template = BitcoinPowTaskOrchestrator.templateStore[task.jobId]
            ?: return ValidationResult(ValidationStatus.CONFLICT, "Template lost")

        val header = ByteArray(80)
        System.arraycopy(hex.parseHex(taskPayload.dataHex), 0, header, 0, 76)
        Utils.uint32ToByteArrayLE(resultPayload.nonce, header, 76)

        val block = reconstructBlock(template, header)
        val submitResult = rpcClient.submitBlock(hex.formatHex(block.bitcoinSerialize()))

        return if (submitResult == null) {
            ValidationResult(ValidationStatus.ACCEPTED)
        } else {
            ValidationResult(ValidationStatus.REJECTED, "RPC rejected block: $submitResult")
        }
    }

    private fun reconstructBlock(template: Map<String, Any>, headerBytes: ByteArray): Block {
        val block = params.defaultSerializer.makeBlock(headerBytes)
        val height = (template["height"] as Number).toLong()
        val coinbaseValue = (template["coinbasevalue"] as Number).toLong()

        val coinbaseTx = Transaction(params)
        val inputScript = ScriptBuilder().data(Utils.encodeMPI(java.math.BigInteger.valueOf(height), false)).build()
        coinbaseTx.addInput(Sha256Hash.ZERO_HASH, -1, inputScript)
        val dummyAddress = LegacyAddress.fromPubKeyHash(params, ByteArray(20))
        coinbaseTx.addOutput(Coin.valueOf(coinbaseValue), dummyAddress)

        val transactions = (template["transactions"] as List<Map<String, Any>>).map { tx ->
            Transaction(params, hex.parseHex(tx["data"] as String))
        }

        block.addTransaction(coinbaseTx)
        transactions.forEach { block.addTransaction(it) }
        return block
    }
}

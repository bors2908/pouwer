package ru.itmo.enterprise.pow.service.orchestrator

import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.NonceRange
import ru.itmo.enterprise.pow.model.Sha256PowTaskPayload
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.service.BitcoinRpcClient
import ru.itmo.enterprise.pow.service.TaskOrchestrator
import ru.itmo.enterprise.pow.service.TaskStore
import java.util.HexFormat
import java.util.UUID
import org.bitcoinj.core.*
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.springframework.stereotype.Component

@Component
class BitcoinPowTaskOrchestrator(
    private val rpcClient: BitcoinRpcClient,
    private val taskStore: TaskStore
) : TaskOrchestrator {
    private val params = RegTestParams.get()
    private val hex = HexFormat.of()
    private val chunkSize = 500_000L
    override val type: JobType = JobType.BITCOIN_RPC_SHA256

    override fun createTask(workerId: String?): Task {
        val template = rpcClient.getBlockTemplate()
        val job = createJobFromTemplate(template)
        
        val jobId = UUID.fromString(job.jobId)
        
        val workerNum = workerId?.toIntOrNull() ?: 0
        val nonceStart = workerNum * chunkSize
        val nonceEnd = nonceStart + chunkSize

        val payload = Sha256PowTaskPayload(
            dataHex = job.headerPrefixHex, // This is 76 bytes
            nonceOffset = 76, // Nonce starts at 76
            nonceIsLE = true,
            targetHex = job.targetHex,
            nonceRange = NonceRange(nonceStart, nonceEnd)
        )

        val task = Task(
            jobId = jobId,
            jobType = type,
            expiresAt = System.currentTimeMillis() + 60_000,
            payload = payload
        )

        // We need to store the block template for reconstruction later
        // Currently Task doesn't have a place for it, and TaskStore only takes Task.
        // We might need a specialized TaskStore or store it in a side-map.
        // For now, let's assume TaskStore can handle it or we use a composite.
        taskStore.save(task)
        // HACK: Store template in a static or shared way if needed, or expand Task.
        // Actually, let's just store it in a map for now.
        templateStore[jobId] = template

        return task
    }

    override fun getTask(jobId: UUID): Task? = taskStore.find(jobId)

    // Copied and adapted from CryptoChallengeService
    private fun createJobFromTemplate(template: Map<String, Any>): ru.itmo.enterprise.pow.model.CryptoJob {
        val version = (template["version"] as Number).toLong()
        val previousBlockHash = (template["previousblockhash"] as String)
        val bits = template["bits"] as String
        val time = (template["curtime"] as Number).toLong()
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
        val allTxs = listOf(coinbaseTx) + transactions
        val merkleRoot = calculateMerkleRoot(allTxs.map { it.txId })

        val header = ByteArray(80)
        Utils.uint32ToByteArrayLE(version, header, 0)
        System.arraycopy(Utils.reverseBytes(hex.parseHex(previousBlockHash)), 0, header, 4, 32)
        System.arraycopy(Utils.reverseBytes(merkleRoot.bytes), 0, header, 36, 32)
        Utils.uint32ToByteArrayLE(time, header, 68)
        System.arraycopy(Utils.reverseBytes(hex.parseHex(bits)), 0, header, 72, 4)

        val headerPrefixHex = hex.formatHex(header, 0, 76)
        val target = Utils.decodeCompactBits(java.lang.Long.parseUnsignedLong(bits, 16))
        val targetBytes = target.toByteArray().let {
            if (it.size > 32) it.copyOfRange(it.size - 32, it.size)
            else ByteArray(32 - it.size) + it
        }

        return ru.itmo.enterprise.pow.model.CryptoJob(
            jobId = UUID.randomUUID().toString(),
            headerPrefixHex = headerPrefixHex,
            targetHex = hex.formatHex(targetBytes),
            blockTemplate = template
        )
    }

    private fun calculateMerkleRoot(hashes: List<Sha256Hash>): Sha256Hash {
        var level = hashes
        while (level.size > 1) {
            val nextLevel = mutableListOf<Sha256Hash>()
            for (i in 0 until level.size step 2) {
                val left = level[i]
                val right = if (i + 1 < level.size) level[i + 1] else level[i]
                nextLevel.add(Sha256Hash.wrapReversed(Sha256Hash.hashTwice(
                    Utils.reverseBytes(left.bytes) + Utils.reverseBytes(right.bytes)
                )))
            }
            level = nextLevel
        }
        return level[0]
    }

    companion object {
        val templateStore = mutableMapOf<UUID, Map<String, Any>>()
    }
}

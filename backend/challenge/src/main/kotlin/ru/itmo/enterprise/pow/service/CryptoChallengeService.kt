package ru.itmo.enterprise.pow.service

import org.bitcoinj.core.*
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.springframework.stereotype.Service
import ru.itmo.enterprise.pow.model.ChallengeResponseCrypto
import ru.itmo.enterprise.pow.model.CryptoJob
import ru.itmo.enterprise.pow.model.ValidateRequestCrypto
import java.util.HexFormat
import java.util.UUID

@Service
class CryptoChallengeService(
    private val rpcClient: BitcoinRpcClient,
    private val jobStore: CryptoJobStore
) {
    private val params = RegTestParams.get()
    private val hex = HexFormat.of()
    private val chunkSize = 500_000L

    fun getChallenge(workerId: Int): ChallengeResponseCrypto {
        var job = jobStore.currentJob
        val now = System.currentTimeMillis()

        if (job == null || job.solved || (now - job.createdAt > 60_000)) {
            job = createNewJob()
            jobStore.currentJob = job
        }

        val nonceStart = workerId.toLong() * chunkSize
        val nonceEnd = nonceStart + chunkSize

        return ChallengeResponseCrypto(
            jobId = job.jobId,
            headerPrefixHex = job.headerPrefixHex,
            targetHex = job.targetHex,
            nonceStart = nonceStart,
            nonceEnd = nonceEnd
        )
    }

    private fun createNewJob(): CryptoJob {
        val template = rpcClient.getBlockTemplate()

        val version = (template["version"] as Number).toLong()
        val previousBlockHash = (template["previousblockhash"] as String)
        val bits = template["bits"] as String
        val time = (template["curtime"] as Number).toLong()
        val height = (template["height"] as Number).toLong()
        val coinbaseValue = (template["coinbasevalue"] as Number).toLong()

        // 1. Build coinbase transaction
        val coinbaseTx = Transaction(params)
        // Coinbase input: block height and some extra data
        val inputScript =
            ScriptBuilder().data(Utils.encodeMPI(java.math.BigInteger.valueOf(height), false)).build()
        coinbaseTx.addInput(Sha256Hash.ZERO_HASH, -1, inputScript)
        // Coinbase output: send reward to a dummy address (or use a real one from template if provided)
        val dummyAddress = LegacyAddress.fromPubKeyHash(params, ByteArray(20)) // dummy regtest addr
        coinbaseTx.addOutput(Coin.valueOf(coinbaseValue), dummyAddress)

        // 2. Compute Merkle Root
        val transactions = (template["transactions"] as List<Map<String, Any>>).map { tx ->
            Transaction(params, hex.parseHex(tx["data"] as String))
        }
        val allTxs = listOf(coinbaseTx) + transactions
        val merkleRoot = buildMerkleRoot(allTxs)

        // 3. Build Header Prefix (76 bytes)
        // version (4) + prevHash (32) + merkleRoot (32) + time (4) + bits (4)
        val header = ByteArray(80)
        Utils.uint32ToByteArrayLE(version, header, 0)
        System.arraycopy(Utils.reverseBytes(hex.parseHex(previousBlockHash)), 0, header, 4, 32)
        System.arraycopy(Utils.reverseBytes(merkleRoot.bytes), 0, header, 36, 32)
        Utils.uint32ToByteArrayLE(time, header, 68)
        System.arraycopy(Utils.reverseBytes(hex.parseHex(bits)), 0, header, 72, 4)

        val headerPrefixHex = hex.formatHex(header, 0, 76)

        // 4. Target
        val target = Utils.decodeCompactBits(java.lang.Long.parseUnsignedLong(bits, 16))
        val targetBytes = target.toByteArray().let {
            if (it.size > 32) it.copyOfRange(it.size - 32, it.size)
            else ByteArray(32 - it.size) + it
        }
        val targetHex = hex.formatHex(targetBytes)

        return CryptoJob(
            jobId = UUID.randomUUID().toString(),
            headerPrefixHex = headerPrefixHex,
            targetHex = targetHex,
            blockTemplate = template,
            solved = false
        )
    }

    private fun buildMerkleRoot(txs: List<Transaction>): Sha256Hash {
        val tree = txs.map { it.txId }
        return calculateMerkleRoot(tree)
    }

    private fun calculateMerkleRoot(hashes: List<Sha256Hash>): Sha256Hash {
        var level = hashes
        while (level.size > 1) {
            val nextLevel = mutableListOf<Sha256Hash>()
            for (i in 0 until level.size step 2) {
                val left = level[i]
                val right = if (i + 1 < level.size) level[i + 1] else level[i]
                nextLevel.add(
                    Sha256Hash.wrapReversed(
                        Sha256Hash.hashTwice(
                            Utils.reverseBytes(left.bytes) + Utils.reverseBytes(right.bytes)
                        )
                    )
                )
            }
            level = nextLevel
        }
        return level[0]
    }

    fun validate(request: ValidateRequestCrypto): Boolean {
        val job = jobStore.currentJob ?: return false
        if (job.jobId != request.jobId || job.solved) return false

        val header = ByteArray(80)
        System.arraycopy(hex.parseHex(job.headerPrefixHex), 0, header, 0, 76)
        Utils.uint32ToByteArrayLE(request.nonce, header, 76)

        val hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(header))
        if (job.targetHex != hex.formatHex(hash.bytes)) {
            // Simplified check: hash should be less than target
            // Actually, we should compare the hash to the target numerically
            val target = java.math.BigInteger(1, hex.parseHex(job.targetHex))
            val hashVal = java.math.BigInteger(1, hash.bytes)
            if (hashVal > target) return false
        }

        // Reconstruct block and submit
        val block = reconstructBlock(job, header)
        val result = rpcClient.submitBlock(hex.formatHex(block.bitcoinSerialize()))
        if (result == null) {
            job.solved = true
            return true
        }
        return false
    }

    private fun reconstructBlock(job: CryptoJob, headerBytes: ByteArray): Block {
        val template = job.blockTemplate
        val block = params.defaultSerializer.makeBlock(headerBytes)

        // We need to add all transactions including the coinbase we built
        // Re-building the exact same coinbase is tricky without storing it.
        // Let's modify CryptoJob to store the coinbase tx hex.
        // Actually, let's just re-build it since we have the template.
        val height = (template["height"] as Number).toLong()
        val coinbaseValue = (template["coinbasevalue"] as Number).toLong()
        val coinbaseTx = Transaction(params)
        val inputScript =
            ScriptBuilder().data(Utils.encodeMPI(java.math.BigInteger.valueOf(height), false)).build()
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

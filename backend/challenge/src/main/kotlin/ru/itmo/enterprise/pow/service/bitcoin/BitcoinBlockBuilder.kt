package ru.itmo.enterprise.pow.service.bitcoin

import org.bitcoinj.core.Block
import org.bitcoinj.core.Coin
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.Utils
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.client.bitcoin.BitcoinBlockTemplate
import ru.itmo.enterprise.pow.model.Sha256PowTaskPayload
import java.math.BigInteger
import java.util.HexFormat

@Component
class BitcoinBlockBuilder {
    private val params = RegTestParams.get()
    private val hex = HexFormat.of()

    fun createTaskPayload(
        template: BitcoinBlockTemplate,
        nonceStart: Long,
        nonceEnd: Long
    ): Sha256PowTaskPayload {
        val coinbaseTx = createCoinbaseTx(template.height, template.coinbaseValue)
        val transactions = template.transactions.map { tx ->
            Transaction(params, hex.parseHex(tx.data))
        }

        val allTransactions = listOf(coinbaseTx) + transactions
        val merkleRoot = calculateMerkleRoot(allTransactions.map { it.txId })

        val header = ByteArray(80)
        Utils.uint32ToByteArrayLE(template.version, header, 0)
        System.arraycopy(Utils.reverseBytes(hex.parseHex(template.previousBlockHash)), 0, header, 4, 32)
        System.arraycopy(Utils.reverseBytes(merkleRoot.bytes), 0, header, 36, 32)
        Utils.uint32ToByteArrayLE(template.curTime, header, 68)
        System.arraycopy(Utils.reverseBytes(hex.parseHex(template.bits)), 0, header, 72, 4)

        val target = Utils.decodeCompactBits(java.lang.Long.parseUnsignedLong(template.bits, 16))
        val targetBytes = target.toByteArray().let {
            if (it.size > 32) it.copyOfRange(it.size - 32, it.size) else ByteArray(32 - it.size) + it
        }

        return Sha256PowTaskPayload(
            dataHex = hex.formatHex(header, 0, 76),
            nonceOffset = 76,
            nonceIsLE = true,
            targetHex = hex.formatHex(targetBytes),
            nonceRange = ru.itmo.enterprise.pow.model.NonceRange(nonceStart, nonceEnd)
        )
    }

    fun reconstructBlock(template: BitcoinBlockTemplate, headerBytes: ByteArray): Block {
        val block = params.defaultSerializer.makeBlock(headerBytes)
        val coinbaseTx = createCoinbaseTx(template.height, template.coinbaseValue)
        val transactions = template.transactions.map { tx ->
            Transaction(params, hex.parseHex(tx.data))
        }
        block.addTransaction(coinbaseTx)
        transactions.forEach { block.addTransaction(it) }
        return block
    }

    private fun createCoinbaseTx(height: Long, coinbaseValue: Long): Transaction {
        val coinbaseTx = Transaction(params)
        val inputScript = ScriptBuilder()
            .data(Utils.encodeMPI(BigInteger.valueOf(height), false))
            .build()
        coinbaseTx.addInput(Sha256Hash.ZERO_HASH, -1, inputScript)
        val dummyAddress = LegacyAddress.fromPubKeyHash(params, ByteArray(20))
        coinbaseTx.addOutput(Coin.valueOf(coinbaseValue), dummyAddress)
        return coinbaseTx
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
                        Sha256Hash.hashTwice(Utils.reverseBytes(left.bytes) + Utils.reverseBytes(right.bytes))
                    )
                )
            }
            level = nextLevel
        }
        return level[0]
    }
}

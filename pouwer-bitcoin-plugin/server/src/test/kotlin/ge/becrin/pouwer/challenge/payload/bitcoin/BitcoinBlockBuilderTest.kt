package ge.becrin.pouwer.challenge.payload.bitcoin

import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.params.RegTestParams
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.HexFormat

class BitcoinBlockBuilderTest {
    private val builder = BitcoinBlockBuilder()
    private val params = RegTestParams.get()
    private val hex = HexFormat.of()

    @Test
    fun testCreateTaskPayloadHandlesSingleTransactionMerkleRoot() {
        val payload = builder.createTaskPayload(createTemplate(listOf(createTransactionHex())), 7, 13)

        assertEquals(7, payload.nonceRange.start)
        assertEquals(13, payload.nonceRange.end)
        assertEquals(true, payload.nonceIsLE)
        assertEquals(76, payload.nonceOffset)
        assertTrue(payload.targetHex.isNotBlank())
    }

    @Test
    fun testCreateTaskPayloadHandlesOddTransactionCountMerkleRoot() {
        val payload = builder.createTaskPayload(
            createTemplate(listOf(createTransactionHex(), createTransactionHex())),
            0,
            100
        )

        assertEquals(0, payload.nonceRange.start)
        assertEquals(100, payload.nonceRange.end)
        assertEquals(true, payload.nonceIsLE)
        assertEquals(76, payload.nonceOffset)
        assertTrue(payload.targetHex.isNotBlank())
    }

    private fun createTemplate(transactions: List<String>): BitcoinBlockTemplate = BitcoinBlockTemplate(
        version = 1,
        previousBlockHash = "00".repeat(32),
        bits = "207fffff",
        curTime = 1_700_000_000,
        height = 1,
        coinbaseValue = 1_0000,
        transactions = transactions.map { BitcoinTemplateTransaction(it) }
    )

    private fun createTransactionHex(): String {
        val transaction = Transaction(params)
        val inputScript = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        transaction.addInput(
            TransactionInput(
                params,
                transaction,
                inputScript,
                TransactionOutPoint(params, -1L, Sha256Hash.ZERO_HASH)
            )
        )
        transaction.addOutput(Coin.valueOf(1_0000), LegacyAddress.fromKey(params, ECKey()))
        return hex.formatHex(transaction.bitcoinSerialize())
    }
}

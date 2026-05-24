package ge.becrin.pouwer.challenge.payload.bitcoin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class BitcoinTemplateStoreTest {
    @Test
    fun testFindReturnsNullForExpiredTemplate() {
        val store = InMemoryBitcoinTemplateStore()
        val jobId = UUID.randomUUID()

        store.save(jobId, createTemplate(), System.currentTimeMillis() - 10_000)

        assertNull(store.find(jobId))
    }

    @Test
    fun testSaveRemovesExpiredTemplatesBeforeStoringNewOne() {
        val store = InMemoryBitcoinTemplateStore()
        val expiredJobId = UUID.randomUUID()
        val activeJobId = UUID.randomUUID()

        store.save(expiredJobId, createTemplate(), System.currentTimeMillis() - 10_000)
        store.save(activeJobId, createTemplate(), System.currentTimeMillis() + 60_000)

        assertNull(store.find(expiredJobId))
        assertEquals(createTemplate(), store.find(activeJobId))
    }

    private fun createTemplate(): BitcoinBlockTemplate = BitcoinBlockTemplate(
        version = 1,
        previousBlockHash = "00".repeat(32),
        bits = "207fffff",
        curTime = 1_700_000_000,
        height = 1,
        coinbaseValue = 1_0000,
        transactions = emptyList()
    )
}

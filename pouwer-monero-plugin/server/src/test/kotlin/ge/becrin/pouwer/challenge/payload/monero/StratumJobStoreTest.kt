package ge.becrin.pouwer.challenge.payload.monero

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StratumJobStoreTest {
    @Test
    fun testParseJobStoresFieldsAndStripsPrefixes() {
        val store = StratumJobStore()
        store.parseJob(
            mapOf(
                "job_id" to "job-1",
                "blob" to "0xdeadbeef",
                "target" to "0x00ff",
                "seed_hash" to "seed-1",
                "height" to "42"
            )
        )

        val job = store.getLatest()

        assertEquals(RawStratumJob("job-1", "deadbeef", "00ff", "seed-1", 42), job)
    }

    @Test
    fun testParseJobAllowsMinimalParams() {
        val store = StratumJobStore()
        store.parseJob(
            mapOf(
                "job_id" to "job-1",
                "blob" to "0xdeadbeef"
            )
        )

        val job = store.getLatest()

        assertEquals("job-1", job?.jobId)
        assertEquals("deadbeef", job?.blob)
        assertNull(job?.targetHex)
        assertNull(job?.seedHash)
        assertNull(job?.height)
    }

    @Test
    fun testParseJobThrowsWhenJobIdMissing() {
        val store = StratumJobStore()
        assertThrows(IllegalArgumentException::class.java) {
            store.parseJob(
                mapOf(
                    "blob" to "0xdeadbeef",
                    "target" to "0x00ff"
                )
            )
        }
    }

    @Test
    fun testParseJobThrowsWhenBlobMissing() {
        val store = StratumJobStore()
        assertThrows(IllegalArgumentException::class.java) {
            store.parseJob(
                mapOf(
                    "job_id" to "job-1",
                    "target" to "0x00ff"
                )
            )
        }
    }
}

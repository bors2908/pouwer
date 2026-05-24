package ge.becrin.pouwer.challenge.payload.monero

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StratumToMoneroConverterTest {
    @Test
    fun testConvertCopiesRawJobFields() {
        val converter = StratumToMoneroConverter()
        val rawJob = RawStratumJob(
            jobId = "job-1",
            blob = "deadbeef",
            targetHex = "00ff",
            seedHash = "seed-1",
            height = 42
        )

        val payload = converter.convert(rawJob)

        assertEquals(rawJob.jobId, payload.stratumJobId)
        assertEquals(rawJob.blob, payload.blob)
        assertEquals(rawJob.targetHex, payload.targetHex)
        assertEquals(rawJob.seedHash, payload.seedHash)
        assertEquals(rawJob.height, payload.height)
    }
}

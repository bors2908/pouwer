package ge.becrin.pouwer.challenge.payload.monero

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.atomic.AtomicReference

class StratumJobStore {
    private val latest = AtomicReference<RawStratumJob?>(null)

    fun set(job: RawStratumJob) {
        log.info { "Received new job: $job" }

        latest.set(job)
    }

    fun getLatest(): RawStratumJob? {
        log.info { "Fetching the latest job" }

        return latest.get()
    }

    fun parseJob(params: Map<String, Any?>) {
        if (params.size < 3) {
            log.warn { "Unexpected job params: $params" }
        }

        val jobId = params["job_id"]?.toString()
            ?: throw IllegalArgumentException("Missing job_id")
        val blobHex = params["blob"]?.toString()?.removePrefix("0x")
            ?: throw IllegalArgumentException("Missing blob")
        val targetRaw = params["target"]?.toString()
        val targetHex = if (targetRaw.isNullOrBlank()) null else targetRaw.removePrefix("0x")
        val seedHash = params["seed_hash"]?.toString()
        val height = params["height"]?.toString()?.toLongOrNull()

        set(
            RawStratumJob(
                jobId = jobId,
                blob = blobHex,
                targetHex = targetHex,
                seedHash = seedHash,
                height = height
            )
        )
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

package ru.itmo.enterprise.pow.client.monero.stratum

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class StratumJobStore {
    private val latest = AtomicReference<RawStratumJob?>(null)

    fun set(job: RawStratumJob) = latest.set(job)
    fun getLatest(): RawStratumJob? = latest.get()

    fun parseJob(params: Map<String, Any?>) {
        // defensive size check
        if (params.size < 3) {
            log.warn { "Unexpected job params: $params" }
        }

        val jobId = params["job_id"]?.toString()!!
        val blobHex = params["blob"].toString().removePrefix("0x")
        val targetRaw = params["target"]?.toString()
        val targetHex = if (targetRaw.isNullOrBlank()) null else targetRaw.removePrefix("0x")
        val seedHash = params["seed_hash"]?.toString()
        val height = params["height"]?.toString()?.toLong()

        val job = RawStratumJob(
            jobId = jobId,
            blob = blobHex,
            targetHex = targetHex,
            seedHash = seedHash,
            height = height,
        )

        set(job)
        log.info { "Stored new stratum job id=$jobId height=$height target=${targetHex ?: "null"}" }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

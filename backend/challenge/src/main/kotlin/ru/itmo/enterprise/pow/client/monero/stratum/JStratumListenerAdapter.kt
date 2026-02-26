package ru.itmo.enterprise.pow.client.monero.stratum

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class JStratumListenerAdapter(
    private val jobStore: StratumJobStore
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Call this from the JStratum notification handler when a JSON-RPC notification arrives.
     * You can pass the raw notification JSON string or the parsed params node.
     *
     * Typical P2Pool mining.notify params (v4):
     * params = [ job_id, blob, target, seed_hash, height, ... ]
     */
    fun onNotification(method: String, params: List<Any>) {
        try {
            when (method) {
                "mining.notify" -> parseNotify(params)
                "mining.set_difficulty" -> parseDifficulty(params)
            }

        } catch (ex: Exception) {
            log.warn("Failed to parse: ${ex.message}")
        }
    }

    private fun parseDifficulty(params: List<Any>) {
        // params[0] = difficulty (some pools send number)
        val diffVal = params.getOrNull(0)?.toString()

        if (!diffVal.isNullOrBlank()) {
            val difficulty = BigInteger(diffVal)
            jobStore.setDifficulty(difficulty)
            log.info("Stratum difficulty set to $difficulty")
        }
    }

    private fun parseNotify(params: List<Any>) {
        // defensive size check
        if (params.size < 3) {
            log.warn("Unexpected mining.notify params: $params")
        }

        val jobId = params.getOrNull(0)?.toString()!!
        val blobHex = params.getOrNull(1).toString().removePrefix("0x")
        val targetRaw = params.getOrNull(2)?.toString()
        val targetHex = if (targetRaw.isNullOrBlank()) null else targetRaw.removePrefix("0x")
        val seedHash = params.getOrNull(3)?.toString()
        val height = params.getOrNull(4)?.toString()?.toLong()

        val job = RawStratumJob(
            jobId = jobId,
            blob = blobHex,
            targetHex = targetHex,
            seedHash = seedHash,
            height = height,
        )

        jobStore.set(job)
        log.info("Stored new stratum job id=$jobId height=$height target=${targetHex ?: "null"}")
    }
}

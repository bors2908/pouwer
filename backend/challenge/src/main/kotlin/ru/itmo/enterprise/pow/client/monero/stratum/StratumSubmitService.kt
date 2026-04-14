package ru.itmo.enterprise.pow.client.monero.stratum

import ge.becrin.kt.stratum.message.ResponseMessage
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Component
class StratumSubmitService(
    private val client: MoneroStratumTcpClient,
    // worker name used when submitting shares; keeps it configurable
    @param:Value($$"${stratum.worker:poctest.worker1}")
    private val workerName: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val defaultTimeoutSec = 10L

    /**
     * Submit a share to the pool.
     * Returns true if pool accepted the share, false otherwise.
     *
     * We send params = [ workerName, jobId, nonceHexLE ]
     */
    fun submitShare(stratumJobId: String?, nonce: Long, hash: String): Boolean {
        val nonceHex = nonceToLEHex(nonce)
        val params = mapOf(
            "id" to client.sessionId,
            "job_id" to stratumJobId,
            "nonce" to nonceHex,
            "result" to hash
        )
        val future: CompletableFuture<ResponseMessage> = client.sendRequest("submit", params)

        log.info("Submitting share. Waiting for pool response [jobId=$stratumJobId]")

        return try {
            val resp = future.get(defaultTimeoutSec, TimeUnit.SECONDS)
            if (resp.error != null) {
                log.info("Pool rejected share: ${resp.error}")
                false
            } else if (resp.result != null) {
                return (resp.result.toJson() as JSONObject).getString("status") == "OK"
            } else {
                //TODO Sure??
                true
            }
        } catch (ex: Exception) {
            log.warn("submitShare failed: ${ex.message}")
            false
        }
    }

    private fun nonceToLEHex(n: Long): String {
        val b0 = (n and 0xff)
        val b1 = (n shr 8) and 0xff
        val b2 = (n shr 16) and 0xff
        val b3 = (n shr 24) and 0xff
        return String.format("%02x%02x%02x%02x", b0, b1, b2, b3)
    }
}

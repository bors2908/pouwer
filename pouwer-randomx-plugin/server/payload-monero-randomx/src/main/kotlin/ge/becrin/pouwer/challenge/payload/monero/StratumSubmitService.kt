package ge.becrin.pouwer.challenge.payload.monero

import ge.becrin.kt.stratum.message.ResponseMessage
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class StratumSubmitService(
    private val client: MoneroStratumTcpClient,
    private val workerName: String = System.getProperty("stratum.worker")
        ?: System.getenv("STRATUM_WORKER")
        ?: "poctest.worker1"
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val defaultTimeoutSec = 10L

    fun submitShare(stratumJobId: String?, nonce: Long, hash: String): Boolean {
        val nonceHex = nonceToLEHex(nonce)
        val params = mapOf(
            "id" to (client.sessionId ?: workerName),
            "job_id" to stratumJobId,
            "nonce" to nonceHex,
            "result" to hash
        )

        val future: CompletableFuture<ResponseMessage> = client.sendRequest("submit", params)

        return try {
            val resp = future.get(defaultTimeoutSec, TimeUnit.SECONDS)
            if (resp.error != null) {
                false
            } else if (resp.result != null) {
                (resp.result.toJson() as JSONObject).optString("status") == "OK"
            } else {
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

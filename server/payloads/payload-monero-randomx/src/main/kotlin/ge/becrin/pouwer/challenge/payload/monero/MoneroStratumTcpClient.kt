package ge.becrin.pouwer.challenge.payload.monero

import ge.becrin.kt.stratum.message.RequestMessage
import ge.becrin.kt.stratum.message.ResponseMessage
import ge.becrin.kt.stratum.transport.AbstractConnectionState
import ge.becrin.kt.stratum.transport.tcp.StratumTcpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class MoneroStratumTcpClient(
    private val host: String = System.getProperty("stratum.host")
        ?: System.getenv("STRATUM_HOST")
        ?: "127.0.0.1",
    private val port: Int = (System.getProperty("stratum.port")
        ?: System.getenv("STRATUM_PORT")
        ?: "3333").toIntOrNull() ?: 3333,
    private val worker: String = System.getProperty("stratum.worker")
        ?: System.getenv("STRATUM_WORKER")
        ?: "poctest.worker1",
    private val password: String = System.getProperty("stratum.password")
        ?: System.getenv("STRATUM_PASSWORD")
        ?: "",
    private val jobStore: StratumJobStore = StratumJobStore()
) : StratumTcpClient() {

    private val pending = ConcurrentHashMap<Long, CompletableFuture<ResponseMessage>>()
    private val idCounter = AtomicLong(1)
    var sessionId: String? = null

    init {
        log.info { "Connecting to Monero Stratum Pool $host:$port" }
        connect(host, port)
    }

    override fun createPostConnectState(): AbstractConnectionState? {
        return object : AbstractConnectionState(this) {
            override fun start() {
                assertConnected()

                registerResponseListener {
                    if (it.id != null) {
                        pending.remove(it.id)?.complete(it)
                    }
                }

                registerNotificationListener {
                    if (it.methodName == "job") {
                        jobStore.parseJob(it.objectParams)
                    }
                }

                sendRequest(
                    "login",
                    mapOf(
                        "login" to worker,
                        "pass" to password,
                        "agent" to "jstratum-client"
                    )
                ).thenAccept { response ->
                    val result = response.result.toJson() as JSONObject
                    if (response.error == null && result.optString("status") == "OK") {
                        sessionId = result.optString("id", null)
                    }

                    if (result.has("job")) {
                        jobStore.parseJob(result.getJSONObject("job").toMap())
                    }
                }
            }
        }
    }

    fun sendRequest(method: String, params: Map<String, Any?>): CompletableFuture<ResponseMessage> {
        val id = idCounter.getAndIncrement()
        val future = CompletableFuture<ResponseMessage>()
        pending[id] = future

        sendRequest(
            RequestMessage(id, method, params),
            ResponseMessage::class.java
        )

        return future
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

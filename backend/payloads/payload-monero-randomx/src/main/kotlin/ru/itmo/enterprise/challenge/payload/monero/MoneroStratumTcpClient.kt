package ru.itmo.enterprise.challenge.payload.monero

import ge.becrin.kt.stratum.message.RequestMessage
import ge.becrin.kt.stratum.message.ResponseMessage
import ge.becrin.kt.stratum.transport.AbstractConnectionState
import ge.becrin.kt.stratum.transport.tcp.StratumTcpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Configuration
open class MoneroStratumTcpClient(
    @param:Value("\${stratum.host:127.0.0.1}") private val host: String,
    @param:Value("\${stratum.port:3333}") private val port: Int,
    @param:Value("\${stratum.worker:worker}") private val worker: String,
    @param:Value("\${stratum.password:}") private val password: String,
    private val jobStore: StratumJobStore
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

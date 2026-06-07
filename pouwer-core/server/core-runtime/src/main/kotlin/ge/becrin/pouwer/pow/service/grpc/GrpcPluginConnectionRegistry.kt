package ge.becrin.pouwer.pow.service.grpc

import ge.becrin.pouwer.plugintransport.v1.MessageType
import ge.becrin.pouwer.plugintransport.v1.OperationType
import ge.becrin.pouwer.plugintransport.v1.PluginEnvelope
import ge.becrin.pouwer.pow.service.PluginStateRegistry
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class GrpcPluginConnectionRegistry(
    private val requestTimeoutMs: Long,
    private val heartbeatLeaseMs: Long,
    private val clock: Clock = Clock.systemUTC()
) {
    private data class Connection(
        val pluginId: String,
        val sessionId: String,
        val outbound: StreamObserver<PluginEnvelope>,
        @Volatile var lastHeartbeatAt: Instant
    )

    private data class PendingRequest(
        val pluginId: String,
        val operationType: OperationType,
        val response: CompletableDeferred<PluginEnvelope> = CompletableDeferred()
    )

    private val connections = PluginStateRegistry<Connection>()
    private val pendingRequests = ConcurrentHashMap<String, PendingRequest>()

    fun register(envelope: PluginEnvelope, outbound: StreamObserver<PluginEnvelope>) {
        connections.replace(envelope.pluginId, Connection(envelope.pluginId, envelope.sessionId, outbound, now()))
            ?.outbound
            ?.onCompleted()
    }

    fun heartbeat(envelope: PluginEnvelope) {
        connections.updateIfPresent(envelope.pluginId) { connection ->
            if (connection.sessionId == envelope.sessionId) {
                connection.lastHeartbeatAt = now()
            }
            connection
        }
    }

    fun unregister(pluginId: String, sessionId: String) {
        connections.removeIf(pluginId) { connection -> connection.sessionId == sessionId }
    }

    fun disconnect(pluginId: String, sessionId: String) = unregister(pluginId, sessionId)

    fun isAvailable(pluginId: String): Boolean = connections.get(pluginId)
        ?.let { connection ->
            connection.lastHeartbeatAt.plusMillis(heartbeatLeaseMs).isAfter(now())
        }
        ?: false

    suspend fun request(pluginId: String, envelope: PluginEnvelope): PluginEnvelope {
        val connection = connections.get(pluginId)
            ?: throw IllegalStateException("Plugin $pluginId is not connected")
        require(isAvailable(pluginId)) { "Plugin $pluginId heartbeat lease expired" }

        val pending = PendingRequest(pluginId, envelope.operationType)
        pendingRequests[envelope.requestId] = pending
        try {
            connection.outbound.onNext(envelope)
            val response = withTimeout(requestTimeoutMs) { pending.response.await() }
            if (response.messageType == MessageType.ERROR) {
                throw IllegalStateException(response.error.message.ifBlank { response.error.code })
            }
            return response
        } finally {
            pendingRequests.remove(envelope.requestId)
        }
    }

    fun complete(envelope: PluginEnvelope) {
        pendingRequests[envelope.requestId]
            ?.takeIf { pending ->
                pending.pluginId == envelope.pluginId && pending.operationType == envelope.operationType
            }
            ?.response
            ?.complete(envelope)
    }

    fun fail(pluginId: String, cause: Throwable) {
        pendingRequests.values
            .filter { it.pluginId == pluginId }
            .forEach { it.response.completeExceptionally(cause) }
    }

    private fun now(): Instant = Instant.now(clock)
}

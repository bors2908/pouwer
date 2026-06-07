package ge.becrin.pouwer.pow.service.grpc

import com.google.protobuf.Timestamp
import ge.becrin.pouwer.plugintransport.v1.MessageType
import ge.becrin.pouwer.plugintransport.v1.OperationType
import ge.becrin.pouwer.plugintransport.v1.PluginEnvelope
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GrpcPluginConnectionStoreTest {
    @Test
    fun `correlates response by request id`() = runBlocking {
        val registry = GrpcPluginConnectionStore(requestTimeoutMs = 1_000, heartbeatLeaseMs = 60_000)
        val observer = RecordingObserver()
        registry.register(registerEnvelope(), observer)

        val request = requestEnvelope("request-1")
        val pending = async { registry.request("sha256", request) }
        yield()

        assertEquals(request, observer.values.single())

        val response = request.toBuilder()
            .setMessageType(MessageType.RESPONSE)
            .build()
        registry.complete(response)

        assertEquals(response, pending.await())
    }

    @Test
    fun `duplicate registration replaces previous stream`() {
        val registry = GrpcPluginConnectionStore(requestTimeoutMs = 1_000, heartbeatLeaseMs = 60_000)
        val first = RecordingObserver()
        val second = RecordingObserver()

        registry.register(registerEnvelope("session-1"), first)
        registry.register(registerEnvelope("session-2"), second)

        assertTrue(first.completed)
        assertTrue(registry.isAvailable("sha256"))
    }

    @Test
    fun `unregister removes matching session`() {
        val registry = GrpcPluginConnectionStore(requestTimeoutMs = 1_000, heartbeatLeaseMs = 60_000)
        registry.register(registerEnvelope("session-1"), RecordingObserver())

        registry.unregister("sha256", "session-1")

        assertFalse(registry.isAvailable("sha256"))
    }

    private fun registerEnvelope(sessionId: String = "session-1"): PluginEnvelope = PluginEnvelope.newBuilder()
        .setSchemaVersion("v1")
        .setPluginId("sha256")
        .setSessionId(sessionId)
        .setMessageType(MessageType.REGISTER)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1))
        .build()

    private fun requestEnvelope(requestId: String): PluginEnvelope = registerEnvelope()
        .toBuilder()
        .setRequestId(requestId)
        .setMessageType(MessageType.REQUEST)
        .setOperationType(OperationType.BUILD_PAYLOAD)
        .build()

    private class RecordingObserver : StreamObserver<PluginEnvelope> {
        val values = mutableListOf<PluginEnvelope>()
        var completed = false

        override fun onNext(value: PluginEnvelope) {
            values += value
        }

        override fun onError(t: Throwable) = throw t

        override fun onCompleted() {
            completed = true
        }
    }
}

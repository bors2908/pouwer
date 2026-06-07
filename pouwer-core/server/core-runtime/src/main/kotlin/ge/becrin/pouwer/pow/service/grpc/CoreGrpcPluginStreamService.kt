package ge.becrin.pouwer.pow.service.grpc

import ge.becrin.pouwer.plugintransport.v1.MessageType
import ge.becrin.pouwer.plugintransport.v1.PluginEnvelope
import ge.becrin.pouwer.plugintransport.v1.PluginTransportStreamServiceGrpc
import ge.becrin.pouwer.challenge.api.PluginRegistration
import ge.becrin.pouwer.challenge.api.PluginTransportMode
import ge.becrin.pouwer.pow.service.CorePluginLifecycle
import ge.becrin.pouwer.pow.service.PluginConnectionSource
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.stub.StreamObserver
import java.time.Instant

class CoreGrpcPluginStreamService(
    private val connectionStore: GrpcPluginConnectionStore,
    private val lifecycle: CorePluginLifecycle,
    private val validator: GrpcPluginEnvelopeValidator
) : PluginTransportStreamServiceGrpc.PluginTransportStreamServiceImplBase() {
    override fun connect(responseObserver: StreamObserver<PluginEnvelope>): StreamObserver<PluginEnvelope> {
        return object : StreamObserver<PluginEnvelope> {
            private var pluginId: String? = null
            private var sessionId: String? = null

            override fun onNext(envelope: PluginEnvelope) {
                try {
                    validator.validate(envelope)
                    pluginId = envelope.pluginId
                    sessionId = envelope.sessionId

                    when (envelope.messageType) {
                        MessageType.REGISTER -> register(envelope, responseObserver)
                        MessageType.HEARTBEAT -> heartbeat(envelope)
                        MessageType.RESPONSE, MessageType.ERROR -> connectionStore.complete(envelope)
                        MessageType.UNREGISTER -> unregister(envelope)
                        else -> throw IllegalArgumentException("Unsupported plugin-origin message type ${envelope.messageType}")
                    }
                } catch (e: Exception) {
                    log.warn(e) { "Invalid gRPC plugin envelope" }
                    responseObserver.onError(e)
                }
            }

            override fun onError(t: Throwable) {
                val id = pluginId
                val session = sessionId
                if (id != null && session != null) {
                    connectionStore.fail(id, t)
                    connectionStore.disconnect(id, session)
                    lifecycle.markUnavailable(id, t)
                }
            }

            override fun onCompleted() {
                val id = pluginId
                val session = sessionId
                if (id != null && session != null) {
                    connectionStore.disconnect(id, session)
                    lifecycle.markUnavailable(id)
                }
                responseObserver.onCompleted()
            }
        }
    }

    private fun register(envelope: PluginEnvelope, responseObserver: StreamObserver<PluginEnvelope>) {
        connectionStore.register(envelope, responseObserver)
        lifecycle.register(
            PluginRegistration(
                id = envelope.pluginId,
                version = envelope.routingMetadataMap["version"].orEmpty(),
                contractVersion = envelope.routingMetadataMap["contractVersion"].orEmpty(),
                baseUrl = envelope.routingMetadataMap["baseUrl"].orEmpty()
            ),
            source(envelope)
        )
    }

    private fun heartbeat(envelope: PluginEnvelope) {
        connectionStore.heartbeat(envelope)
        lifecycle.heartbeat(envelope.pluginId, envelope.timestamp.toInstant(), source(envelope))
    }

    private fun unregister(envelope: PluginEnvelope) {
        connectionStore.unregister(envelope.pluginId, envelope.sessionId)
        lifecycle.unregister(envelope.pluginId, source(envelope))
    }

    private fun source(envelope: PluginEnvelope): PluginConnectionSource = PluginConnectionSource(
        mode = PluginTransportMode.GRPC,
        sessionId = envelope.sessionId,
        baseUrl = envelope.routingMetadataMap["baseUrl"]
    )

    private fun com.google.protobuf.Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanos.toLong())

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

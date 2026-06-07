package ge.becrin.pouwer.pow.service.grpc

import ge.becrin.pouwer.plugintransport.v1.MessageType
import ge.becrin.pouwer.plugintransport.v1.PluginEnvelope
import ge.becrin.pouwer.plugintransport.v1.PluginTransportStreamServiceGrpc
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.stub.StreamObserver

class CoreGrpcPluginStreamService(
    private val registry: GrpcPluginConnectionRegistry,
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
                        MessageType.REGISTER -> registry.register(envelope, responseObserver)
                        MessageType.HEARTBEAT -> registry.heartbeat(envelope)
                        MessageType.RESPONSE, MessageType.ERROR -> registry.complete(envelope)
                        MessageType.UNREGISTER -> registry.unregister(envelope.pluginId, envelope.sessionId)
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
                    registry.fail(id, t)
                    registry.disconnect(id, session)
                }
            }

            override fun onCompleted() {
                val id = pluginId
                val session = sessionId
                if (id != null && session != null) {
                    registry.disconnect(id, session)
                }
                responseObserver.onCompleted()
            }
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
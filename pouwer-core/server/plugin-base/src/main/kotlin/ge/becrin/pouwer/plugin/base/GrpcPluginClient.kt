package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PluginHeartbeat
import ge.becrin.pouwer.challenge.api.PluginRegistration
import ge.becrin.pouwer.challenge.api.PluginValidationRequest
import ge.becrin.pouwer.challenge.api.ProtoJson
import ge.becrin.pouwer.plugintransport.v1.ErrorDetails
import ge.becrin.pouwer.plugintransport.v1.MessageType
import ge.becrin.pouwer.plugintransport.v1.OperationType
import ge.becrin.pouwer.plugintransport.v1.PluginEnvelope
import ge.becrin.pouwer.plugintransport.v1.PluginTransportStreamServiceGrpc
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.StreamObserver
import tools.jackson.databind.ObjectMapper
import java.util.UUID

class GrpcPluginClient(
    private val properties: PluginBaseProperties,
    private val descriptor: PluginDescriptor,
    private val payloadPlugin: PluginPayloadService,
    private val mapper: ObjectMapper
) : PluginCoreTransportAdapter {
    private lateinit var channel: ManagedChannel
    private lateinit var outbound: StreamObserver<PluginEnvelope>
    private val sessionId = UUID.randomUUID().toString()

    @Volatile
    private var running = false

    override fun start() {
        if (running) {
            return
        }

        channel = NettyChannelBuilder.forAddress(properties.grpc.host, properties.grpc.port)
            .usePlaintext()
            .build()

        val stub = PluginTransportStreamServiceGrpc.newStub(channel)

        outbound = stub.connect(object : StreamObserver<PluginEnvelope> {
            override fun onNext(envelope: PluginEnvelope) = handle(envelope)
            override fun onError(t: Throwable) = log.error(t) { "Core gRPC stream failed for ${descriptor.pluginId}" }
            override fun onCompleted() = log.info { "Core gRPC stream completed for ${descriptor.pluginId}" }
        })

        running = true
    }

    override fun stop() {
        if (!running) {
            return
        }

        unregister(descriptor.pluginId)
        outbound.onCompleted()
        channel.shutdown()
        running = false
    }

    override fun register(registration: PluginRegistration) {
        send(MessageType.REGISTER, registration)
    }

    override fun heartbeat(heartbeat: PluginHeartbeat) {
        if (running) {
            send(MessageType.HEARTBEAT)
        }
    }

    override fun unregister(pluginId: String) {
        if (running) {
            send(MessageType.UNREGISTER)
        }
    }

    private fun handle(envelope: PluginEnvelope) {
        if (envelope.messageType != MessageType.REQUEST) {
            return
        }

        val response = try {
            val payload = when (envelope.operationType) {
                OperationType.BUILD_PAYLOAD -> payloadPlugin.buildPayload(
                    mapper.readValue(toJson(envelope), PayloadBuildRequest::class.java)
                )

                OperationType.VALIDATE_RESULT -> mapper.readValue(toJson(envelope), PluginValidationRequest::class.java).let {
                    payloadPlugin.validate(it.task, it.result)
                }

                OperationType.HEALTH -> mapOf("status" to "UP", "pluginId" to descriptor.pluginId)
                else -> error("Unsupported operation ${envelope.operationType}")
            }
            envelope.toBuilder()
                .setMessageType(MessageType.RESPONSE)
                .setSessionId(sessionId)
                .setTimestamp(ProtoJson.timestampNow())
                .setPayload(ProtoJson.toStruct(mapper, payload))
                .build()
        } catch (e: Exception) {
            envelope.toBuilder()
                .setMessageType(MessageType.ERROR)
                .setSessionId(sessionId)
                .setTimestamp(ProtoJson.timestampNow())
                .setError(ErrorDetails.newBuilder().setCode("PLUGIN_ERROR").setMessage(e.message ?: e.javaClass.name))
                .build()
        }

        outbound.onNext(response)
    }

    private fun send(type: MessageType, registration: PluginRegistration? = null) {
        outbound.onNext(
            PluginEnvelope.newBuilder()
                .setSchemaVersion(properties.grpc.schemaVersion)
                .setPluginId(descriptor.pluginId)
                .setSessionId(sessionId)
                .setMessageType(type)
                .setTimestamp(ProtoJson.timestampNow())
                .putRoutingMetadata("contractVersion", CHALLENGE_PLUGIN_CONTRACT_VERSION)
                .putRoutingMetadata("version", registration?.version ?: descriptor.version)
                .putRoutingMetadata("baseUrl", registration?.baseUrl ?: descriptor.baseUrl)
                .build()
        )
    }

    private fun toJson(envelope: PluginEnvelope): String = ProtoJson.toJson(envelope.payload)

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

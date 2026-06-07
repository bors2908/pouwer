package ge.becrin.pouwer.pow.service.grpc

import ge.becrin.pouwer.plugintransport.v1.MessageType
import ge.becrin.pouwer.plugintransport.v1.OperationType
import ge.becrin.pouwer.plugintransport.v1.PluginEnvelope

class GrpcPluginEnvelopeValidator(
    private val schemaVersion: String
) {
    fun validate(envelope: PluginEnvelope) {
        require(envelope.schemaVersion == schemaVersion) { "Unsupported schema version: ${envelope.schemaVersion}" }
        require(envelope.pluginId.isNotBlank()) { "pluginId is required" }
        require(envelope.sessionId.isNotBlank()) { "sessionId is required" }
        require(envelope.hasTimestamp()) { "timestamp is required" }
        require(envelope.messageType != MessageType.MESSAGE_TYPE_UNSPECIFIED) { "messageType is required" }

        when (envelope.messageType) {
            MessageType.REQUEST, MessageType.RESPONSE, MessageType.ERROR -> {
                require(envelope.requestId.isNotBlank()) { "requestId is required for ${envelope.messageType}" }
                require(envelope.operationType != OperationType.OPERATION_TYPE_UNSPECIFIED) {
                    "operationType is required for ${envelope.messageType}"
                }
            }
            else -> Unit
        }
    }
}
package ge.becrin.pouwer.pow.service.grpc

import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginTransport
import ge.becrin.pouwer.challenge.api.PluginValidationRequest
import ge.becrin.pouwer.challenge.api.ProtoJson
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.plugintransport.v1.MessageType
import ge.becrin.pouwer.plugintransport.v1.OperationType
import ge.becrin.pouwer.plugintransport.v1.PluginEnvelope
import ge.becrin.pouwer.pow.service.PluginTransportException
import tools.jackson.databind.ObjectMapper
import java.util.UUID

class GrpcPluginTransport(
    private val registry: GrpcPluginConnectionRegistry,
    private val mapper: ObjectMapper,
    private val schemaVersion: String
) : PluginTransport {
    override suspend fun buildPayload(plugin: PluginMetadata, request: PayloadBuildRequest): Task =
        call(plugin.id, OperationType.BUILD_PAYLOAD, request, Task::class.java)

    override suspend fun validateResult(plugin: PluginMetadata, task: Task, result: ResultMessage): ValidationResult =
        call(plugin.id, OperationType.VALIDATE_RESULT, PluginValidationRequest(task, result), ValidationResult::class.java)

    override suspend fun health(plugin: PluginMetadata): Boolean = registry.isAvailable(plugin.id)

    private suspend fun <T> call(pluginId: String, operation: OperationType, payload: Any, type: Class<T>): T {
        return try {
            val request = PluginEnvelope.newBuilder()
                .setSchemaVersion(schemaVersion)
                .setPluginId(pluginId)
                .setSessionId("core")
                .setRequestId(UUID.randomUUID().toString())
                .setMessageType(MessageType.REQUEST)
                .setOperationType(operation)
                .setTimestamp(ProtoJson.timestampNow())
                .setPayload(ProtoJson.toStruct(mapper, payload))
                .build()
            ProtoJson.fromStruct(mapper, registry.request(pluginId, request).payload, type)
        } catch (e: Exception) {
            throw PluginTransportException("Failed to call gRPC plugin $pluginId", e)
        }
    }
}
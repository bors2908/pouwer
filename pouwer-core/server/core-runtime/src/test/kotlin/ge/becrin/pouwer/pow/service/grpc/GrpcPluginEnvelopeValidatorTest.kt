package ge.becrin.pouwer.pow.service.grpc

import com.google.protobuf.Timestamp
import ge.becrin.pouwer.plugintransport.v1.MessageType
import ge.becrin.pouwer.plugintransport.v1.OperationType
import ge.becrin.pouwer.plugintransport.v1.PluginEnvelope
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GrpcPluginEnvelopeValidatorTest {
    private val validator = GrpcPluginEnvelopeValidator("v1")

    @Test
    fun `accepts valid request envelope`() {
        assertDoesNotThrow {
            validator.validate(
                envelopeBuilder()
                    .setRequestId("request-1")
                    .setMessageType(MessageType.REQUEST)
                    .setOperationType(OperationType.BUILD_PAYLOAD)
                    .build()
            )
        }
    }

    @Test
    fun `rejects unsupported schema version`() {
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(envelopeBuilder().setSchemaVersion("v2").build())
        }
    }

    @Test
    fun `rejects response without request id`() {
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(
                envelopeBuilder()
                    .setMessageType(MessageType.RESPONSE)
                    .setOperationType(OperationType.BUILD_PAYLOAD)
                    .build()
            )
        }
    }

    private fun envelopeBuilder(): PluginEnvelope.Builder = PluginEnvelope.newBuilder()
        .setSchemaVersion("v1")
        .setPluginId("sha256")
        .setSessionId("session-1")
        .setMessageType(MessageType.REGISTER)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1))
}
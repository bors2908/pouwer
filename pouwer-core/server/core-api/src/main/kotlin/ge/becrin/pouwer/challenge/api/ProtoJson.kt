package ge.becrin.pouwer.challenge.api

import com.google.protobuf.Struct
import com.google.protobuf.Timestamp
import com.google.protobuf.util.JsonFormat
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Instant

object ProtoJson {
    private val structPrinter = JsonFormat.printer().omittingInsignificantWhitespace()
    private val structParser = JsonFormat.parser()

    fun toStruct(mapper: ObjectMapper, value: Any): Struct = Struct.newBuilder()
        .also { structParser.merge(mapper.writeValueAsString(value), it) }
        .build()

    fun toJson(struct: Struct): String = structPrinter.print(struct)

    fun toJsonNode(mapper: ObjectMapper, struct: Struct): JsonNode = mapper.readTree(toJson(struct))

    fun <T> fromStruct(mapper: ObjectMapper, struct: Struct, type: Class<T>): T = mapper.readValue(toJson(struct), type)

    fun timestampNow(): Timestamp = Instant.now().let {
        Timestamp.newBuilder().setSeconds(it.epochSecond).setNanos(it.nano).build()
    }
}
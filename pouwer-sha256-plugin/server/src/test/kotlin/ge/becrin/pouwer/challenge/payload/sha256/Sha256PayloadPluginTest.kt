package ge.becrin.pouwer.challenge.payload.sha256

import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.module.kotlin.jacksonObjectMapper
import ge.becrin.pouwer.challenge.api.NonceRange
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

class Sha256PayloadPluginTest {
    private val mapper = jacksonObjectMapper()
    private val hex = HexFormat.of()

    @Test
    fun testCreateTaskBuildsPayloadWithNonceRange() {
        val plugin = Sha256PayloadPlugin(targetHex = TARGET_HEX, nonceRangeSize = 1000)

        val task = plugin.createTask(workerId = "worker-1", nowMillis = 1000, taskTtlMillis = 5000)
        val payload = mapper.treeToValue(task.payload, Sha256PowTaskPayload::class.java)

        assertEquals(Sha256PayloadPlugin.PLUGIN_ID, task.pluginId)
        assertEquals(6000, task.expiresAt)
        assertEquals(0, payload.nonceRange.start)
        assertEquals(1000, payload.nonceRange.end)
        assertEquals(TARGET_HEX, payload.targetHex)
        assertEquals(0, payload.nonceOffset)
        assertEquals(false, payload.nonceIsLE)
        assertEquals(64, payload.dataHex.length)
    }

    @Test
    fun testValidateRejectsPluginMismatch() {
        val plugin = Sha256PayloadPlugin(targetHex = TARGET_HEX, nonceRangeSize = 1000)
        val task = Task(
            jobId = UUID.randomUUID(),
            pluginId = "other-plugin",
            expiresAt = System.currentTimeMillis() + 60_000,
            payload = JsonNodeFactory.instance.objectNode()
        )

        val result = plugin.validate(task, createResultPayload(task, Sha256PowResultPayload(1, "00")))

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    @Test
    fun testValidateRejectsInvalidTaskPayload() {
        val plugin = Sha256PayloadPlugin(targetHex = TARGET_HEX, nonceRangeSize = 1000)
        val task = Task(
            jobId = UUID.randomUUID(),
            pluginId = Sha256PayloadPlugin.PLUGIN_ID,
            expiresAt = System.currentTimeMillis() + 60_000,
            payload = JsonNodeFactory.instance.objectNode().put("bad", "payload")
        )

        val result = plugin.validate(task, createResultPayload(task, Sha256PowResultPayload(1, "00")))

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    @Test
    fun testValidateRejectsInvalidResultPayload() {
        val plugin = Sha256PayloadPlugin(targetHex = TARGET_HEX, nonceRangeSize = 1000)
        val taskPayload = createTaskPayload(NonceRange(0, 10))
        val task = createTask(taskPayload)
        val result = ResultMessage(
            jobId = task.jobId,
            pluginId = task.pluginId,
            payload = JsonNodeFactory.instance.objectNode().put("bad", "payload"),
            durationMs = null,
            attempts = null
        )

        val validation = plugin.validate(task, result)

        assertEquals(ValidationStatus.REJECTED, validation.status)
    }

    @Test
    fun testValidateReturnsConflictForNonceOutsideRange() {
        val plugin = Sha256PayloadPlugin(targetHex = TARGET_HEX, nonceRangeSize = 1000)
        val taskPayload = createTaskPayload(NonceRange(0, 10))
        val task = createTask(taskPayload)
        val resultPayload = Sha256PowResultPayload(10, "00")

        val validation = plugin.validate(task, createResultPayload(task, resultPayload))

        assertEquals(ValidationStatus.CONFLICT, validation.status)
    }

    @Test
    fun testValidateReturnsConflictForNonceBelowRange() {
        val plugin = Sha256PayloadPlugin(targetHex = TARGET_HEX, nonceRangeSize = 1000)
        val taskPayload = createTaskPayload(NonceRange(1, 10))
        val task = createTask(taskPayload)
        val resultPayload = Sha256PowResultPayload(0, "00")

        val validation = plugin.validate(task, createResultPayload(task, resultPayload))

        assertEquals(ValidationStatus.CONFLICT, validation.status)
    }

    @Test
    fun testValidateAcceptsValidHash() {
        val plugin = Sha256PayloadPlugin(targetHex = MAX_TARGET_HEX, nonceRangeSize = 1000)
        val taskPayload = createTaskPayload(NonceRange(0, 100))
        val task = createTask(taskPayload)
        val nonce = 42L
        val hashHex = computeHashHex(taskPayload.dataHex, nonce, taskPayload.nonceOffset, taskPayload.nonceIsLE)
        val resultPayload = Sha256PowResultPayload(nonce, hashHex)

        val validation = plugin.validate(task, createResultPayload(task, resultPayload))

        assertEquals(ValidationStatus.ACCEPTED, validation.status)
    }

    @Test
    fun testValidateRejectsInvalidHash() {
        val plugin = Sha256PayloadPlugin(targetHex = MAX_TARGET_HEX, nonceRangeSize = 1000)
        val taskPayload = createTaskPayload(NonceRange(0, 100))
        val task = createTask(taskPayload)
        val resultPayload = Sha256PowResultPayload(1, "deadbeef")

        val validation = plugin.validate(task, createResultPayload(task, resultPayload))

        assertEquals(ValidationStatus.REJECTED, validation.status)
    }

    @Test
    fun testValidateRejectsInvalidNonceOffset() {
        val plugin = Sha256PayloadPlugin(targetHex = MAX_TARGET_HEX, nonceRangeSize = 1000)
        val taskPayload = createTaskPayload(NonceRange(0, 100), nonceOffset = 40)
        val task = createTask(taskPayload)
        val resultPayload = Sha256PowResultPayload(1, "00")

        val validation = plugin.validate(task, createResultPayload(task, resultPayload))

        assertEquals(ValidationStatus.REJECTED, validation.status)
    }

    @Test
    fun testValidateAcceptsLittleEndianNonce() {
        val plugin = Sha256PayloadPlugin(targetHex = MAX_TARGET_HEX, nonceRangeSize = 1000)
        val taskPayload = createTaskPayload(NonceRange(0, 100), nonceIsLE = true, nonceOffset = 4)
        val task = createTask(taskPayload)
        val nonce = 42L
        val hashHex = computeHashHex(taskPayload.dataHex, nonce, taskPayload.nonceOffset, taskPayload.nonceIsLE)
        val resultPayload = Sha256PowResultPayload(nonce, hashHex)

        val validation = plugin.validate(task, createResultPayload(task, resultPayload))

        assertEquals(ValidationStatus.ACCEPTED, validation.status)
    }

    @Test
    fun testValidateRejectsMalformedHashHex() {
        val plugin = Sha256PayloadPlugin(targetHex = MAX_TARGET_HEX, nonceRangeSize = 1000)
        val taskPayload = createTaskPayload(NonceRange(0, 100))
        val task = createTask(taskPayload)
        val resultPayload = Sha256PowResultPayload(1, "not-hex")

        val validation = plugin.validate(task, createResultPayload(task, resultPayload))

        assertEquals(ValidationStatus.REJECTED, validation.status)
    }

    private fun createTaskPayload(
        nonceRange: NonceRange,
        nonceIsLE: Boolean = false,
        nonceOffset: Int = 0,
        targetHex: String = MAX_TARGET_HEX,
        dataHex: String = DEFAULT_DATA_HEX
    ): Sha256PowTaskPayload {
        return Sha256PowTaskPayload(
            dataHex = dataHex,
            nonceOffset = nonceOffset,
            nonceIsLE = nonceIsLE,
            targetHex = targetHex,
            nonceRange = nonceRange
        )
    }

    private fun createTask(taskPayload: Sha256PowTaskPayload): Task = Task(
        jobId = UUID.randomUUID(),
        pluginId = Sha256PayloadPlugin.PLUGIN_ID,
        expiresAt = System.currentTimeMillis() + 60_000,
        payload = mapper.valueToTree(taskPayload)
    )

    private fun createResultPayload(task: Task, resultPayload: Sha256PowResultPayload): ResultMessage = ResultMessage(
        jobId = task.jobId,
        pluginId = task.pluginId,
        payload = mapper.valueToTree(resultPayload),
        durationMs = null,
        attempts = null
    )

    private fun computeHashHex(
        dataHex: String,
        nonce: Long,
        nonceOffset: Int,
        nonceIsLE: Boolean
    ): String {
        val data = hex.parseHex(dataHex)
        if (nonceOffset < 0 || nonceOffset + 4 > data.size) {
            throw IllegalArgumentException("Invalid nonce offset")
        }
        val working = data.copyOf()
        if (nonceIsLE) {
            working[nonceOffset] = (nonce and 0xFF).toByte()
            working[nonceOffset + 1] = ((nonce shr 8) and 0xFF).toByte()
            working[nonceOffset + 2] = ((nonce shr 16) and 0xFF).toByte()
            working[nonceOffset + 3] = ((nonce shr 24) and 0xFF).toByte()
        } else {
            working[nonceOffset] = ((nonce shr 24) and 0xFF).toByte()
            working[nonceOffset + 1] = ((nonce shr 16) and 0xFF).toByte()
            working[nonceOffset + 2] = ((nonce shr 8) and 0xFF).toByte()
            working[nonceOffset + 3] = (nonce and 0xFF).toByte()
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(digest.digest(working))
        return hex.formatHex(hash)
    }

    companion object {
        private const val TARGET_HEX = "0000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        private const val MAX_TARGET_HEX = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        private val DEFAULT_DATA_HEX = HexFormat.of().formatHex(ByteArray(32) { it.toByte() })
    }
}

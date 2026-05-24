package ge.becrin.pouwer.challenge.payload.bitcoin

import tools.jackson.module.kotlin.jacksonObjectMapper
import ge.becrin.pouwer.challenge.api.NonceRange
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

class BitcoinRpcPayloadPluginTest {
    private val mapper = jacksonObjectMapper()
    private val hex = HexFormat.of()

    @Test
    fun testCreateTaskUsesWorkerIdToComputeNonceRange() {
        val rpc = FakeBitcoinNodeClient(template = createTemplate(), submitResult = null)
        val store = InMemoryBitcoinTemplateStore()
        val plugin = BitcoinRpcPayloadPlugin(rpc, store, BitcoinBlockBuilder())
        val now = System.currentTimeMillis()

        val task = plugin.createTask(workerId = "2", nowMillis = now, taskTtlMillis = 60_000)
        val payload = mapper.treeToValue(task.payload, BitcoinSha256TaskPayload::class.java)

        assertEquals(BitcoinRpcPayloadPlugin.PLUGIN_ID, task.pluginId)
        assertEquals(now + 60_000, task.expiresAt)
        assertEquals(1_000_000L, payload.nonceRange.start)
        assertEquals(1_500_000L, payload.nonceRange.end)
        assertNotNull(store.find(task.jobId))
    }

    @Test
    fun testCreateTaskFallsBackToZeroNonceRangeForNonNumericWorkerId() {
        val rpc = FakeBitcoinNodeClient(template = createTemplate(), submitResult = null)
        val plugin = BitcoinRpcPayloadPlugin(rpc, InMemoryBitcoinTemplateStore(), BitcoinBlockBuilder())
        val now = System.currentTimeMillis()

        val task = plugin.createTask(workerId = "worker-x", nowMillis = now, taskTtlMillis = 60_000)
        val payload = mapper.treeToValue(task.payload, BitcoinSha256TaskPayload::class.java)

        assertEquals(0L, payload.nonceRange.start)
        assertEquals(500_000L, payload.nonceRange.end)
    }

    @Test
    fun testValidateRejectsPluginMismatch() {
        val plugin = BitcoinRpcPayloadPlugin(
            FakeBitcoinNodeClient(createTemplate(), null),
            InMemoryBitcoinTemplateStore(),
            BitcoinBlockBuilder()
        )
        val task = Task(UUID.randomUUID(), "other", 1_000, mapper.createObjectNode())

        val result = plugin.validate(task, createResult(task, BitcoinSha256ResultPayload(1, "00")))

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    @Test
    fun testValidateReturnsConflictForNonceOutsideRange() {
        val plugin = BitcoinRpcPayloadPlugin(
            FakeBitcoinNodeClient(createTemplate(), null),
            InMemoryBitcoinTemplateStore(),
            BitcoinBlockBuilder()
        )
        val payload = createTaskPayload(nonceRange = NonceRange(1, 2))
        val task = createTask(payload)

        val result = plugin.validate(task, createResult(task, BitcoinSha256ResultPayload(2, "00")))

        assertEquals(ValidationStatus.CONFLICT, result.status)
    }

    @Test
    fun testValidateRejectsInvalidHash() {
        val plugin = BitcoinRpcPayloadPlugin(
            FakeBitcoinNodeClient(createTemplate(), null),
            InMemoryBitcoinTemplateStore(),
            BitcoinBlockBuilder()
        )
        val payload = createTaskPayload(targetHex = MAX_TARGET, nonceRange = NonceRange(0, 10))
        val task = createTask(payload)

        val result = plugin.validate(task, createResult(task, BitcoinSha256ResultPayload(1, "deadbeef")))

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    @Test
    fun testValidateReturnsConflictWhenTemplateMissing() {
        val plugin = BitcoinRpcPayloadPlugin(
            FakeBitcoinNodeClient(createTemplate(), null),
            InMemoryBitcoinTemplateStore(),
            BitcoinBlockBuilder()
        )
        val payload = createTaskPayload(targetHex = MAX_TARGET, nonceRange = NonceRange(0, 10))
        val task = createTask(payload)
        val nonce = 1L
        val hash = computeHash(payload, nonce)

        val result = plugin.validate(task, createResult(task, BitcoinSha256ResultPayload(nonce, hash)))

        assertEquals(ValidationStatus.CONFLICT, result.status)
    }

    @Test
    fun testValidateAcceptsWhenRpcSubmitReturnsNull() {
        val template = createTemplate()
        val store = InMemoryBitcoinTemplateStore()
        val plugin = BitcoinRpcPayloadPlugin(
            FakeBitcoinNodeClient(template, null),
            store,
            BitcoinBlockBuilder()
        )
        val payload = createTaskPayload(targetHex = MAX_TARGET, nonceRange = NonceRange(0, 10))
        val task = createTask(payload)
        store.save(task.jobId, template, System.currentTimeMillis() + 60_000)
        val nonce = 1L
        val hash = computeHash(payload, nonce)

        val result = plugin.validate(task, createResult(task, BitcoinSha256ResultPayload(nonce, hash)))

        assertEquals(ValidationStatus.ACCEPTED, result.status)
    }

    @Test
    fun testValidateRejectsWhenRpcSubmitReturnsReason() {
        val template = createTemplate()
        val store = InMemoryBitcoinTemplateStore()
        val plugin = BitcoinRpcPayloadPlugin(
            FakeBitcoinNodeClient(template, "high-hash"),
            store,
            BitcoinBlockBuilder()
        )
        val payload = createTaskPayload(targetHex = MAX_TARGET, nonceRange = NonceRange(0, 10))
        val task = createTask(payload)
        store.save(task.jobId, template, System.currentTimeMillis() + 60_000)
        val nonce = 1L
        val hash = computeHash(payload, nonce)

        val result = plugin.validate(task, createResult(task, BitcoinSha256ResultPayload(nonce, hash)))

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    private fun createTaskPayload(
        targetHex: String = MAX_TARGET,
        nonceRange: NonceRange = NonceRange(0, 100)
    ): BitcoinSha256TaskPayload = BitcoinSha256TaskPayload(
        dataHex = hex.formatHex(ByteArray(76) { it.toByte() }),
        nonceOffset = 0,
        nonceIsLE = true,
        targetHex = targetHex,
        nonceRange = nonceRange
    )

    private fun createTask(payload: BitcoinSha256TaskPayload): Task = Task(
        jobId = UUID.randomUUID(),
        pluginId = BitcoinRpcPayloadPlugin.PLUGIN_ID,
        expiresAt = System.currentTimeMillis() + 60_000,
        payload = mapper.valueToTree(payload)
    )

    private fun createResult(task: Task, payload: BitcoinSha256ResultPayload): ResultMessage = ResultMessage(
        jobId = task.jobId,
        pluginId = task.pluginId,
        payload = mapper.valueToTree(payload),
        durationMs = null,
        attempts = null
    )

    private fun computeHash(taskPayload: BitcoinSha256TaskPayload, nonce: Long): String {
        val data = hex.parseHex(taskPayload.dataHex).copyOf()
        data[taskPayload.nonceOffset] = (nonce and 0xFF).toByte()
        data[taskPayload.nonceOffset + 1] = ((nonce shr 8) and 0xFF).toByte()
        data[taskPayload.nonceOffset + 2] = ((nonce shr 16) and 0xFF).toByte()
        data[taskPayload.nonceOffset + 3] = ((nonce shr 24) and 0xFF).toByte()
        val digest = MessageDigest.getInstance("SHA-256")
        return hex.formatHex(digest.digest(digest.digest(data)))
    }

    private fun createTemplate(): BitcoinBlockTemplate = BitcoinBlockTemplate(
        version = 1,
        previousBlockHash = "00".repeat(32),
        bits = "207fffff",
        curTime = 1_700_000_000,
        height = 1,
        coinbaseValue = 1_0000,
        transactions = emptyList()
    )

    private class FakeBitcoinNodeClient(
        private val template: BitcoinBlockTemplate,
        private val submitResult: String?
    ) : BitcoinNodeClient {
        override fun getBlockTemplate(): BitcoinBlockTemplate = template
        override fun submitBlock(blockHex: String): String? = submitResult
    }

    companion object {
        private const val MAX_TARGET = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
    }
}

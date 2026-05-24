package ge.becrin.pouwer.challenge.payload.monero

import tools.jackson.module.kotlin.jacksonObjectMapper
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class MoneroRandomXPayloadPluginTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun testCreateTaskThrowsWhenNoJobAvailable() {
        val plugin = MoneroRandomXPayloadPlugin(StratumJobStore(), StratumToMoneroConverter(), FakeShareSubmitter(true))

        assertThrows(IllegalStateException::class.java) {
            plugin.createTask(workerId = null, nowMillis = 1_000, taskTtlMillis = 60_000)
        }
    }

    @Test
    fun testCreateTaskBuildsTaskFromLatestJob() {
        val store = StratumJobStore()
        store.set(RawStratumJob("job-1", "deadbeef", "ff", "seed-1", 42))
        val plugin = MoneroRandomXPayloadPlugin(store, StratumToMoneroConverter(), FakeShareSubmitter(true))

        val task = plugin.createTask(workerId = null, nowMillis = 1_000, taskTtlMillis = 60_000)
        val payload = mapper.treeToValue(task.payload, MoneroRandomXTaskPayload::class.java)

        assertEquals(MoneroRandomXPayloadPlugin.PLUGIN_ID, task.pluginId)
        assertEquals(61_000, task.expiresAt)
        assertEquals("job-1", payload.stratumJobId)
    }

    @Test
    fun testValidateRejectsPluginMismatch() {
        val plugin = MoneroRandomXPayloadPlugin(StratumJobStore(), StratumToMoneroConverter(), FakeShareSubmitter(true))
        val task = Task(UUID.randomUUID(), "other", 1_000, mapper.createObjectNode())

        val result = plugin.validate(task, createResult(task, MoneroRandomXResultPayload("x", 1, "00")))

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    @Test
    fun testValidateRejectsInvalidNonceRange() {
        val plugin = MoneroRandomXPayloadPlugin(StratumJobStore(), StratumToMoneroConverter(), FakeShareSubmitter(true))
        val task = createTask(createTaskPayload(targetHex = "ff", stratumJobId = "job-1"))

        val low = plugin.validate(task, createResult(task, MoneroRandomXResultPayload("x", -1, "00")))
        val high = plugin.validate(task, createResult(task, MoneroRandomXResultPayload("x", 0x1_0000_0000, "00")))

        assertEquals(ValidationStatus.REJECTED, low.status)
        assertEquals(ValidationStatus.REJECTED, high.status)
    }

    @Test
    fun testValidateRejectsInvalidHashAndTargetCases() {
        val plugin = MoneroRandomXPayloadPlugin(StratumJobStore(), StratumToMoneroConverter(), FakeShareSubmitter(true))
        val baseTask = createTask(createTaskPayload(targetHex = "ff", stratumJobId = "job-1"))

        val invalidHash = plugin.validate(baseTask, createResult(baseTask, MoneroRandomXResultPayload("x", 1, "not-hex")))
        val missingTarget = plugin.validate(
            createTask(createTaskPayload(targetHex = null, stratumJobId = "job-1")),
            createResult(baseTask, MoneroRandomXResultPayload("x", 1, "00"))
        )
        val invalidTarget = plugin.validate(
            createTask(createTaskPayload(targetHex = "zz", stratumJobId = "job-1")),
            createResult(baseTask, MoneroRandomXResultPayload("x", 1, "00"))
        )

        assertEquals(ValidationStatus.REJECTED, invalidHash.status)
        assertEquals(ValidationStatus.REJECTED, missingTarget.status)
        assertEquals(ValidationStatus.REJECTED, invalidTarget.status)
    }

    @Test
    fun testValidateRejectsHashAboveTarget() {
        val plugin = MoneroRandomXPayloadPlugin(StratumJobStore(), StratumToMoneroConverter(), FakeShareSubmitter(true))
        val task = createTask(createTaskPayload(targetHex = "00", stratumJobId = "job-1"))

        val result = plugin.validate(task, createResult(task, MoneroRandomXResultPayload("x", 1, "ff")))

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    @Test
    fun testValidateAcceptsWhenShareSubmitted() {
        val plugin = MoneroRandomXPayloadPlugin(StratumJobStore(), StratumToMoneroConverter(), FakeShareSubmitter(true))
        val task = createTask(createTaskPayload(targetHex = "ffffffff", stratumJobId = "job-1"))

        val result = plugin.validate(task, createResult(task, MoneroRandomXResultPayload("x", 1, "01")))

        assertEquals(ValidationStatus.ACCEPTED, result.status)
    }

    @Test
    fun testValidateRejectsWhenPoolRejectsShare() {
        val plugin = MoneroRandomXPayloadPlugin(StratumJobStore(), StratumToMoneroConverter(), FakeShareSubmitter(false))
        val task = createTask(createTaskPayload(targetHex = "ffffffff", stratumJobId = "job-1"))

        val result = plugin.validate(task, createResult(task, MoneroRandomXResultPayload("x", 1, "01")))

        assertEquals(ValidationStatus.REJECTED, result.status)
    }

    private fun createTaskPayload(targetHex: String?, stratumJobId: String?): MoneroRandomXTaskPayload =
        MoneroRandomXTaskPayload(
            id = "task-1",
            blob = "deadbeef",
            targetHex = targetHex,
            height = 42,
            stratumJobId = stratumJobId,
            seedHash = "seed"
        )

    private fun createTask(payload: MoneroRandomXTaskPayload): Task = Task(
        jobId = UUID.randomUUID(),
        pluginId = MoneroRandomXPayloadPlugin.PLUGIN_ID,
        expiresAt = System.currentTimeMillis() + 60_000,
        payload = mapper.valueToTree(payload)
    )

    private fun createResult(task: Task, payload: MoneroRandomXResultPayload): ResultMessage = ResultMessage(
        jobId = task.jobId,
        pluginId = task.pluginId,
        payload = mapper.valueToTree(payload),
        durationMs = null,
        attempts = null
    )

    private data class FakeShareSubmitter(private val result: Boolean) : ShareSubmitter {
        override fun submitShare(stratumJobId: String?, nonce: Long, hash: String): Boolean = result
    }
}

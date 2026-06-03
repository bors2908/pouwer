package ge.becrin.pouwer.pow.service.store

import ge.becrin.pouwer.challenge.api.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.JsonNodeFactory
import java.util.UUID

class InMemoryTaskStoreTest {

    @Test
    fun testEvictsTaskByExpiresAt() {
        var now = 1_000L
        val task = task(expiresAt = 1_500L)
        val taskStore = InMemoryTaskStore { now }

        taskStore.save(task)

        assertEquals(task, taskStore.find(task.jobId))

        now = 1_500L

        assertNull(taskStore.find(task.jobId))
    }

    @Test
    fun testDoesNotReturnAlreadyExpiredTask() {
        val now = 1_000L
        val task = task(expiresAt = now)
        val taskStore = InMemoryTaskStore { now }

        taskStore.save(task)

        assertNull(taskStore.find(task.jobId))
    }

    @Test
    fun testRemoveReturnsTaskAndEvictsIt() {
        val task = task(expiresAt = 2_000L)
        val taskStore = InMemoryTaskStore { 1_000L }

        taskStore.save(task)

        assertEquals(task, taskStore.remove(task.jobId))
        assertNull(taskStore.find(task.jobId))
    }

    private fun task(expiresAt: Long): Task = Task(
        jobId = UUID.randomUUID(),
        pluginId = "pow-test-sha256",
        expiresAt = expiresAt,
        payload = JsonNodeFactory.instance.objectNode()
    )
}
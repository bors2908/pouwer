package ge.becrin.pouwer.pow.service.store

import ge.becrin.pouwer.challenge.api.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.JsonNodeFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class CachingRedisTaskStoreTest {

    @Test
    fun testSaveFindRemoveLifecycle() {
        val repository = CountingTaskRepository()
        val store = CachingRedisTaskStore(repository, CaffeineTaskCache { 1_000L })
        val task = task(expiresAt = 10_000L)

        store.save(task)

        assertEquals(task, store.find(task.jobId))
        assertEquals(task, store.remove(task.jobId))
        assertNull(store.find(task.jobId))
    }

    @Test
    fun testFindCacheHitDoesNotHitRedis() {
        val repository = CountingTaskRepository()
        val store = CachingRedisTaskStore(repository, CaffeineTaskCache { 1_000L })
        val task = task(expiresAt = 10_000L)

        store.save(task)
        repository.findCalls.set(0)

        // L1 was populated by save -> Redis must not be queried.
        assertEquals(task, store.find(task.jobId))
        assertEquals(0, repository.findCalls.get())
    }

    @Test
    fun testFindCacheMissPopulatesL1() {
        val repository = CountingTaskRepository()
        val task = task(expiresAt = 10_000L)
        // Task only exists in the shared repository, not in this node's L1.
        repository.store[task.jobId] = task

        val store = CachingRedisTaskStore(repository, CaffeineTaskCache { 1_000L })

        assertEquals(task, store.find(task.jobId))
        assertEquals(1, repository.findCalls.get())

        // Second find served from L1.
        assertEquals(task, store.find(task.jobId))
        assertEquals(1, repository.findCalls.get())
    }

    @Test
    fun testRemoveIsAtomicAndPreventsReplay() {
        val repository = CountingTaskRepository()
        val store = CachingRedisTaskStore(repository, CaffeineTaskCache { 1_000L })
        val task = task(expiresAt = 10_000L)
        store.save(task)

        assertEquals(task, store.remove(task.jobId))
        // Replay: second remove yields null -> CONFLICT semantics.
        assertNull(store.remove(task.jobId))
    }

    private fun task(expiresAt: Long): Task = Task(
        jobId = UUID.randomUUID(),
        pluginId = "pow-test-sha256",
        expiresAt = expiresAt,
        payload = JsonNodeFactory.instance.objectNode()
    )

    private class CountingTaskRepository : TaskRepository {
        val store = mutableMapOf<UUID, Task>()
        val findCalls = AtomicInteger(0)

        override fun save(task: Task) {
            store[task.jobId] = task
        }

        override fun find(jobId: UUID): Task? {
            findCalls.incrementAndGet()
            return store[jobId]
        }

        override fun getAndRemove(jobId: UUID): Task? = store.remove(jobId)
    }
}

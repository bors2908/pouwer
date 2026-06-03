package ge.becrin.pouwer.pow.service.store

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.pow.service.TaskStore
import java.util.UUID
import java.util.concurrent.TimeUnit

class InMemoryTaskStore(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : TaskStore {
    private val tasks = Caffeine.newBuilder()
        .ticker { TimeUnit.MILLISECONDS.toNanos(currentTimeMillis()) }
        .expireAfter(TaskExpiry(currentTimeMillis))
        .build<UUID, Task>()

    override fun save(task: Task) {
        tasks.put(task.jobId, task)
    }

    override fun find(jobId: UUID): Task? = tasks.getIfPresent(jobId)

    override fun remove(jobId: UUID): Task? = tasks.asMap().remove(jobId)

    private class TaskExpiry(
        private val currentTimeMillis: () -> Long
    ) : Expiry<UUID, Task> {
        override fun expireAfterCreate(key: UUID, value: Task, currentTime: Long): Long = value.ttlNanos()

        override fun expireAfterUpdate(key: UUID, value: Task, currentTime: Long, currentDuration: Long): Long = value.ttlNanos()

        override fun expireAfterRead(key: UUID, value: Task, currentTime: Long, currentDuration: Long): Long = currentDuration

        private fun Task.ttlNanos(): Long {
            val ttlMillis = expiresAt - currentTimeMillis()
            return if (ttlMillis <= 0) {
                0
            } else {
                TimeUnit.MILLISECONDS.toNanos(ttlMillis)
            }
        }
    }
}

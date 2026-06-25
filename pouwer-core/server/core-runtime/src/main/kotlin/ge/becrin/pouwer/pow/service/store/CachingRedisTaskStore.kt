package ge.becrin.pouwer.pow.service.store

import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.pow.service.TaskStore
import java.util.UUID

class CachingRedisTaskStore(
    private val repository: TaskRepository,
    private val l1: CaffeineTaskCache = CaffeineTaskCache()
) : TaskStore {

    override fun save(task: Task) {
        repository.save(task)
        l1.put(task)
    }

    override fun find(jobId: UUID): Task? {
        l1.getIfPresent(jobId)?.let { return it }
        val task = repository.find(jobId) ?: return null
        l1.put(task)
        return task
    }

    override fun remove(jobId: UUID): Task? {
        val removed = repository.getAndRemove(jobId)
        l1.invalidate(jobId)
        return removed
    }
}

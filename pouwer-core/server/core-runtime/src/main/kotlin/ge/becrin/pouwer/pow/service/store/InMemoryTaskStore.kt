package ge.becrin.pouwer.pow.service.store

import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.pow.service.TaskStore
import java.util.UUID

class InMemoryTaskStore(
    currentTimeMillis: () -> Long = System::currentTimeMillis
) : TaskStore {
    private val cache = CaffeineTaskCache(currentTimeMillis)

    override fun save(task: Task) = cache.put(task)

    override fun find(jobId: UUID): Task? = cache.getIfPresent(jobId)

    override fun remove(jobId: UUID): Task? = cache.remove(jobId)
}

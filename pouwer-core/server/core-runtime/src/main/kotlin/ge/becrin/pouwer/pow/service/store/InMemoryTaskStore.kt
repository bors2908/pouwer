package ge.becrin.pouwer.pow.service.store

import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.pow.service.TaskStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryTaskStore : TaskStore {
    private val tasks = ConcurrentHashMap<UUID, Task>()

    override fun save(task: Task) {
        tasks[task.jobId] = task
    }

    override fun find(jobId: UUID): Task? = tasks[jobId]

    override fun remove(jobId: UUID): Task? = tasks.remove(jobId)
}

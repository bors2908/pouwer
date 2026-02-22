package ru.itmo.enterprise.pow.service.store

import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.service.TaskStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryTaskStore : TaskStore {
    private val tasks = ConcurrentHashMap<UUID, Task>()

    override fun save(task: Task) {
        tasks[task.jobId] = task
    }

    override fun find(jobId: UUID): Task? = tasks[jobId]
}

package ru.itmo.enterprise.pow.service.orchestrator

import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.model.TaskPayload
import ru.itmo.enterprise.pow.service.TaskOrchestrator
import ru.itmo.enterprise.pow.service.TaskStore
import java.util.UUID

abstract class AbstractTaskOrchestrator(
    protected val taskStore: TaskStore,
    protected val taskTtlMillis: Long
) : TaskOrchestrator {

    override fun createTask(workerId: String?): Task {
        val task = Task(
            jobId = UUID.randomUUID(),
            jobType = type,
            expiresAt = System.currentTimeMillis() + taskTtlMillis,
            payload = buildPayload(workerId)
        )

        taskStore.save(task)
        onTaskCreated(task, workerId)
        return task
    }

    protected open fun buildPayload(workerId: String?): TaskPayload {
        throw UnsupportedOperationException("buildPayload(workerId) is not implemented")
    }

    protected open fun onTaskCreated(task: Task, workerId: String?) = Unit
}

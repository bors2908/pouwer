package ru.itmo.enterprise.pow.service

import ru.itmo.enterprise.pow.model.Task
import java.util.UUID

interface TaskStore {
    fun save(task: Task)
    fun find(jobId: UUID): Task?
}

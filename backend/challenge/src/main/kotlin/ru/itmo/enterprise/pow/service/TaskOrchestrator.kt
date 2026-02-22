package ru.itmo.enterprise.pow.service

import ru.itmo.enterprise.pow.model.Task
import java.util.UUID

interface TaskOrchestrator {
    fun createTask(workerId: String?): Task
    fun getTask(jobId: UUID): Task?
}

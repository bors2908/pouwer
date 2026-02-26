package ru.itmo.enterprise.pow.service

import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.Task
import java.util.UUID

interface TaskOrchestrator {
    val type: JobType
    fun createTask(workerId: String?): Task
    fun getTask(jobId: UUID): Task?
}

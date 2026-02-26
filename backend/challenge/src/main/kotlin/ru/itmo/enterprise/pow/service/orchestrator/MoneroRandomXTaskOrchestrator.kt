package ru.itmo.enterprise.pow.service.orchestrator

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.client.monero.stratum.StratumJobStore
import ru.itmo.enterprise.pow.client.monero.stratum.StratumToMoneroConverter
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.MoneroRandomXTaskPayload
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.service.TaskOrchestrator
import ru.itmo.enterprise.pow.service.TaskStore
import java.util.UUID

@Component
class MoneroRandomXTaskOrchestrator(
    private val taskStore: TaskStore,
    private val jobStore: StratumJobStore,
    private val converter: StratumToMoneroConverter
) : TaskOrchestrator {
    private val log = LoggerFactory.getLogger(javaClass)

    override val type: JobType = JobType.MONERO_RANDOMX

    override fun createTask(workerId: String?): Task {
        val jobId = UUID.randomUUID()
        val payload = generatePayload(workerId)

        val task = Task(
            jobId = jobId,
            jobType = type,
            expiresAt = System.currentTimeMillis() + 60_000,
            payload = payload
        )

        taskStore.save(task)
        return task
    }

    override fun getTask(jobId: UUID): Task? = taskStore.find(jobId)

    private fun generatePayload(workerId: String?): MoneroRandomXTaskPayload {
        val rawJob = jobStore.getLatest()
            ?: throw IllegalStateException("No stratum job available")

        return converter.convert(rawJob)
    }
}

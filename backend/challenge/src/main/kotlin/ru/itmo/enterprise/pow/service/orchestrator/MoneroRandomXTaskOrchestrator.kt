package ru.itmo.enterprise.pow.service.orchestrator

import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.config.PowProperties
import ru.itmo.enterprise.pow.client.monero.stratum.StratumJobStore
import ru.itmo.enterprise.pow.client.monero.stratum.StratumToMoneroConverter
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.TaskPayload
import ru.itmo.enterprise.pow.service.TaskStore

@Component
class MoneroRandomXTaskOrchestrator(
    taskStore: TaskStore,
    powProperties: PowProperties,
    private val jobStore: StratumJobStore,
    private val converter: StratumToMoneroConverter
) : AbstractTaskOrchestrator(taskStore, powProperties.ttlSeconds * 1000) {
    override val type: JobType = JobType.MONERO_RANDOMX

    override fun buildPayload(workerId: String?): TaskPayload {
        val rawJob = jobStore.getLatest()
            ?: throw IllegalStateException("No stratum job available")

        return converter.convert(rawJob)
    }
}

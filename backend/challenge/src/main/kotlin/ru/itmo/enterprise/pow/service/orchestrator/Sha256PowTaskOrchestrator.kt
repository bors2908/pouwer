package ru.itmo.enterprise.pow.service.orchestrator

import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.NonceRange
import ru.itmo.enterprise.pow.model.Sha256PowTaskPayload
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.service.TaskOrchestrator
import ru.itmo.enterprise.pow.service.TaskStore
import java.util.HexFormat
import java.util.UUID

@Component
class Sha256PowTaskOrchestrator(
    private val taskStore: TaskStore
) : TaskOrchestrator {
    private val hex = HexFormat.of()
    override val type: JobType = JobType.POW_TEST_SHA256

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

    private fun generatePayload(workerId: String?): Sha256PowTaskPayload {
        // Generate synthetic data (32 bytes)
        val data = ByteArray(32)
        java.util.Random().nextBytes(data)
        
        // Target: leading 16 bits zero (difficulty 16)
        val target = "0000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

        return Sha256PowTaskPayload(
            dataHex = hex.formatHex(data),
            nonceOffset = 0,
            nonceIsLE = false,
            targetHex = target,
            nonceRange = NonceRange(0, 1_000_000)
        )
    }
}

package ru.itmo.enterprise.pow.service.orchestrator

import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.config.PowProperties
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.NonceRange
import ru.itmo.enterprise.pow.model.Sha256PowTaskPayload
import ru.itmo.enterprise.pow.model.TaskPayload
import ru.itmo.enterprise.pow.service.TaskStore
import java.util.HexFormat
import kotlin.random.Random

@Component
class Sha256PowTaskOrchestrator(
    taskStore: TaskStore,
    powProperties: PowProperties
) : AbstractTaskOrchestrator(taskStore, powProperties.ttlSeconds * 1000) {
    private val hex = HexFormat.of()
    override val type: JobType = JobType.POW_TEST_SHA256

    override fun buildPayload(workerId: String?): TaskPayload {
        // Generate synthetic data (32 bytes)
        val data = ByteArray(32)
        Random.nextBytes(data)

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

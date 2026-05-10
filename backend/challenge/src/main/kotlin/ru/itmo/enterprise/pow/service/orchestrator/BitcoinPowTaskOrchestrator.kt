package ru.itmo.enterprise.pow.service.orchestrator

import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.client.BitcoinRpcClient
import ru.itmo.enterprise.pow.config.PowProperties
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.service.TaskStore
import ru.itmo.enterprise.pow.service.bitcoin.BitcoinBlockBuilder
import ru.itmo.enterprise.pow.service.bitcoin.BitcoinTemplateStore
import java.util.UUID

@Component
class BitcoinPowTaskOrchestrator(
    private val rpcClient: BitcoinRpcClient,
    taskStore: TaskStore,
    powProperties: PowProperties,
    private val templateStore: BitcoinTemplateStore,
    private val blockBuilder: BitcoinBlockBuilder
) : AbstractTaskOrchestrator(taskStore, powProperties.ttlSeconds * 1000) {
    private val chunkSize = 500_000L
    override val type: JobType = JobType.BITCOIN_RPC_SHA256

    override fun createTask(workerId: String?): Task {
        val template = rpcClient.getBlockTemplate()
        val jobId = UUID.randomUUID()
        val expiresAt = System.currentTimeMillis() + taskTtlMillis
        val workerNum = workerId?.toLongOrNull() ?: 0L
        val nonceStart = workerNum * chunkSize
        val nonceEnd = nonceStart + chunkSize
        val payload = blockBuilder.createTaskPayload(
            template = template,
            nonceStart = nonceStart,
            nonceEnd = nonceEnd
        )
        val task = Task(
            jobId = jobId,
            jobType = type,
            expiresAt = expiresAt,
            payload = payload
        )
        taskStore.save(task)
        templateStore.save(jobId, template, expiresAt)
        return task
    }
}

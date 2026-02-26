package ru.itmo.enterprise.pow.service.orchestrator

import org.springframework.stereotype.Component
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.MoneroRandomXTaskPayload
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.service.MoneroRpcClient
import ru.itmo.enterprise.pow.service.TaskOrchestrator
import ru.itmo.enterprise.pow.service.TaskStore
import java.math.BigInteger
import java.util.UUID

@Component
class MoneroRandomXTaskOrchestrator(
    private val taskStore: TaskStore,
    private val moneroRpcClient: MoneroRpcClient
) : TaskOrchestrator {
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
        // call RPC
        val template = moneroRpcClient.getBlockTemplate(workerId, reserveSize = 0)

        // choose the blob the miner expects: prefer blockhashing_blob, fall back to blocktemplate_blob
        val blob = template.blockhashingBlob ?: throw RuntimeException("Block template missing both blockhashing_blob")

        val difficultyBigInt = template.difficulty
        if (difficultyBigInt <= BigInteger.ZERO) {
            // defensive: if difficulty missing or zero, return payload without difficulty/target
            return MoneroRandomXTaskPayload(
                id = UUID.randomUUID().toString(),
                blob = blob,
                difficulty = null,
                targetHex = null
            )
        }
        // produce difficulty decimal string for the worker
        val difficultyStr = difficultyBigInt.toString()

        // compute target = (2^256 - 1) / difficulty
        val max = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)
        val target = max.divide(difficultyBigInt)

        // convert target to 32-byte big-endian array and hex
        val targetBytes = to32BytesBigEndian(target)
        val targetHex = bytesToHex(targetBytes) // 64 hex chars, big-endian, no 0x prefix

        return MoneroRandomXTaskPayload(
            id = UUID.randomUUID().toString(),
            blob = blob,
            difficulty = difficultyStr,
            targetHex = targetHex
        )
    }

    private fun to32BytesBigEndian(value: BigInteger): ByteArray {
        val raw = value.toByteArray() // big-endian two's complement
        val out = ByteArray(32)
        if (raw.size <= 32) {
            // copy to rightmost bytes (preserve big-endian ordering)
            System.arraycopy(raw, 0, out, 32 - raw.size, raw.size)
        } else {
            // raw is longer than 32, take last 32 bytes
            System.arraycopy(raw, raw.size - 32, out, 0, 32)
        }
        return out
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}

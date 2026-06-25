package ge.becrin.pouwer.pow.service.store

import ge.becrin.pouwer.challenge.api.Task
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.UUID
import tools.jackson.databind.ObjectMapper

class RedisTaskRepository(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val keyPrefix: String = "pouwer:task:",
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : TaskRepository {

    private fun key(jobId: UUID): String = "$keyPrefix$jobId"

    override fun save(task: Task) {
        val ttlMs = task.expiresAt - currentTimeMillis()
        if (ttlMs <= 0) {
            return
        }
        val value = objectMapper.writeValueAsString(task)
        redisTemplate.opsForValue().set(key(task.jobId), value, Duration.ofMillis(ttlMs))
    }

    override fun find(jobId: UUID): Task? {
        val value = redisTemplate.opsForValue().get(key(jobId)) ?: return null
        return objectMapper.readValue(value, Task::class.java)
    }

    override fun getAndRemove(jobId: UUID): Task? {
        val value = redisTemplate.opsForValue().getAndDelete(key(jobId)) ?: return null
        return objectMapper.readValue(value, Task::class.java)
    }
}

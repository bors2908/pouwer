package ge.becrin.pouwer.pow.config

import ge.becrin.pouwer.pow.service.TaskStore
import ge.becrin.pouwer.pow.service.store.CachingRedisTaskStore
import ge.becrin.pouwer.pow.service.store.InMemoryTaskStore
import ge.becrin.pouwer.pow.service.store.RedisTaskRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.ObjectMapper

@Configuration
open class TaskStoreConfiguration {

    @Bean
    @ConditionalOnProperty(
        prefix = "pouwer.task-store",
        name = ["type"],
        havingValue = "in-memory",
        matchIfMissing = true
    )
    open fun inMemoryTaskStore(): TaskStore = InMemoryTaskStore()

    @Bean
    @ConditionalOnProperty(
        prefix = "pouwer.task-store",
        name = ["type"],
        havingValue = "redis"
    )
    open fun redisTaskStore(
        redisTemplate: StringRedisTemplate,
        objectMapper: ObjectMapper,
        properties: TaskStoreProperties
    ): TaskStore = CachingRedisTaskStore(
        RedisTaskRepository(redisTemplate, objectMapper, properties.redis.keyPrefix)
    )
}

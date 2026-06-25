package ge.becrin.pouwer.pow.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pouwer.task-store")
data class TaskStoreProperties(
    var type: TaskStoreType = TaskStoreType.IN_MEMORY,
    var redis: Redis = Redis()
) {
    data class Redis(
        var keyPrefix: String = "pouwer:task:"
    )
}

enum class TaskStoreType {
    IN_MEMORY,
    REDIS
}

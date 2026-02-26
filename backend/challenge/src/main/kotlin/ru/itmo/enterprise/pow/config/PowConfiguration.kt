package ru.itmo.enterprise.pow.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.itmo.enterprise.pow.service.LeaseManager
import ru.itmo.enterprise.pow.service.TaskStore
import ru.itmo.enterprise.pow.service.lease.SimpleLeaseManager
import ru.itmo.enterprise.pow.service.store.InMemoryTaskStore

@Configuration
open class PowConfiguration {

    @Bean
    open fun taskStore(): TaskStore = InMemoryTaskStore()

    @Bean
    open fun leaseManager(): LeaseManager = SimpleLeaseManager()
}

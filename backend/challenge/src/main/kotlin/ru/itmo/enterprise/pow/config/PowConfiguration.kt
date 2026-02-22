package ru.itmo.enterprise.pow.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.itmo.enterprise.pow.model.Sha256PowResultPayload
import ru.itmo.enterprise.pow.model.Sha256PowTaskPayload
import ru.itmo.enterprise.pow.service.*
import ru.itmo.enterprise.pow.service.lease.SimpleLeaseManager
import ru.itmo.enterprise.pow.service.orchestrator.BitcoinPowTaskOrchestrator
import ru.itmo.enterprise.pow.service.orchestrator.Sha256PowTaskOrchestrator
import ru.itmo.enterprise.pow.service.result.BitcoinPowResultService
import ru.itmo.enterprise.pow.service.result.Sha256PowResultService
import ru.itmo.enterprise.pow.service.store.InMemoryTaskStore
import ru.itmo.enterprise.pow.service.validator.Sha256Validator

@Configuration
open class PowConfiguration {

    @Bean
    open fun taskStore(): TaskStore = InMemoryTaskStore()

    @Bean
    open fun leaseManager(): LeaseManager = SimpleLeaseManager()

    @Bean
    open fun sha256Validator(): PowValidator<Sha256PowTaskPayload, Sha256PowResultPayload> = Sha256Validator()

    @Configuration
    @ConditionalOnProperty(name = ["pow.mode"], havingValue = "bitcoin")
    open class BitcoinPowConfig {
        @Bean
        open fun taskOrchestrator(rpcClient: BitcoinRpcClient, taskStore: TaskStore): TaskOrchestrator =
            BitcoinPowTaskOrchestrator(rpcClient, taskStore)

        @Bean
        open fun resultService(
            taskStore: TaskStore,
            leaseManager: LeaseManager,
            validator: PowValidator<Sha256PowTaskPayload, Sha256PowResultPayload>,
            rpcClient: BitcoinRpcClient
        ): ResultService =
            BitcoinPowResultService(taskStore, leaseManager, validator, rpcClient)
    }

    @Configuration
    @ConditionalOnProperty(name = ["pow.mode"], havingValue = "test", matchIfMissing = true)
    open class TestPowConfig {
        @Bean
        open fun taskOrchestrator(taskStore: TaskStore): TaskOrchestrator =
            Sha256PowTaskOrchestrator(taskStore)

        @Bean
        open fun resultService(
            taskStore: TaskStore,
            leaseManager: LeaseManager,
            validator: PowValidator<Sha256PowTaskPayload, Sha256PowResultPayload>
        ): ResultService =
            Sha256PowResultService(taskStore, leaseManager, validator)
    }
}

package ru.itmo.enterprise.pow.service.result

import org.springframework.stereotype.Service
import ru.itmo.enterprise.pow.model.*
import ru.itmo.enterprise.pow.service.LeaseManager
import ru.itmo.enterprise.pow.service.PowValidator
import ru.itmo.enterprise.pow.service.ResultService
import ru.itmo.enterprise.pow.service.TaskStore

@Service
class MoneroRandomXResultService(
    private val taskStore: TaskStore,
    private val leaseManager: LeaseManager,
    private val validator: PowValidator<MoneroRandomXTaskPayload, MoneroRandomXResultPayload>
) : ResultService {
    override val type: JobType = JobType.MONERO_RANDOMX

    override fun handleResult(result: ResultMessage): ValidationResult {
        TODO("Not yet implemented")
    }
}

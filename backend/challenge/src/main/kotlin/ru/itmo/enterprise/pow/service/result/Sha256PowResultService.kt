package ru.itmo.enterprise.pow.service.result

import org.springframework.stereotype.Service
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.Sha256PowResultPayload
import ru.itmo.enterprise.pow.model.Sha256PowTaskPayload
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.model.ValidationResult
import ru.itmo.enterprise.pow.service.LeaseManager
import ru.itmo.enterprise.pow.service.PowValidator
import ru.itmo.enterprise.pow.service.TaskStore

@Service
class Sha256PowResultService(
    private val leaseManager: LeaseManager,
    taskStore: TaskStore,
    private val validator: PowValidator<Sha256PowTaskPayload, Sha256PowResultPayload>
) : BaseResultService<Sha256PowTaskPayload, Sha256PowResultPayload>(
    taskStore = taskStore,
    taskPayloadClass = Sha256PowTaskPayload::class,
    resultPayloadClass = Sha256PowResultPayload::class
) {
    override val type: JobType = JobType.POW_TEST_SHA256

    override fun validate(
        task: Task,
        taskPayload: Sha256PowTaskPayload,
        resultPayload: Sha256PowResultPayload
    ): ValidationResult {
        if (!leaseManager.isNonceAllowed(task, resultPayload.nonce))
            return conflict("Nonce outside lease")

        val valid = validator.verify(
            taskPayload = taskPayload,
            resultPayload = resultPayload
        )

        return if (valid) {
            accepted()
        } else {
            rejected("Hash invalid")
        }
    }
}

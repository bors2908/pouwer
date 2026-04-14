package ru.itmo.enterprise.pow.service.result

import org.springframework.stereotype.Service
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.MoneroRandomXResultPayload
import ru.itmo.enterprise.pow.model.MoneroRandomXTaskPayload
import ru.itmo.enterprise.pow.model.ResultMessage
import ru.itmo.enterprise.pow.model.ValidationResult
import ru.itmo.enterprise.pow.model.ValidationStatus
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
        val task = taskStore.find(result.jobId)
            ?: return ValidationResult(ValidationStatus.CONFLICT, "Unknown job")

        val resultPayload = result.payload as? MoneroRandomXResultPayload
            ?: return ValidationResult(ValidationStatus.REJECTED, "Invalid payload type")

        //if (!leaseManager.isNonceAllowed(task, resultPayload.nonce))
        //    return ValidationResult(ValidationStatus.CONFLICT, "Nonce outside lease")

        val taskPayload = task.payload as? MoneroRandomXTaskPayload
            ?: return ValidationResult(ValidationStatus.REJECTED, "Invalid task payload type")

        val valid = validator.verify(taskPayload, resultPayload)

        return if (!valid) {
            ValidationResult(ValidationStatus.REJECTED, "Hash invalid")
        } else {
            ValidationResult(ValidationStatus.ACCEPTED)
        }
    }
}

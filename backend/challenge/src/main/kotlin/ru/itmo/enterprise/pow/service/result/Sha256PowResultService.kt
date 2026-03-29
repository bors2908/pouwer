package ru.itmo.enterprise.pow.service.result

import org.springframework.stereotype.Service
import ru.itmo.enterprise.pow.model.*
import ru.itmo.enterprise.pow.service.LeaseManager
import ru.itmo.enterprise.pow.service.PowValidator
import ru.itmo.enterprise.pow.service.ResultService
import ru.itmo.enterprise.pow.service.TaskStore

@Service
class Sha256PowResultService(
    private val taskStore: TaskStore,
    private val leaseManager: LeaseManager,
    private val validator: PowValidator<Sha256PowTaskPayload, Sha256PowResultPayload>
) : ResultService {
    override val type: JobType = JobType.POW_TEST_SHA256

    override fun handleResult(result: ResultMessage): ValidationResult {
        val task = taskStore.find(result.jobId)
            ?: return ValidationResult(ValidationStatus.CONFLICT, "Unknown job")

        if (task.jobType != result.payload.jobType)
            return ValidationResult(ValidationStatus.REJECTED, "JobType mismatch")

        val resultPayload = result.payload as? Sha256PowResultPayload
            ?: return ValidationResult(ValidationStatus.REJECTED, "Invalid payload type")

        if (!leaseManager.isNonceAllowed(task, resultPayload.nonce))
            return ValidationResult(ValidationStatus.CONFLICT, "Nonce outside lease")

        val taskPayload = task.payload as? Sha256PowTaskPayload
            ?: return ValidationResult(ValidationStatus.REJECTED, "Invalid task payload type")

        val valid = validator.verify(
            taskPayload = taskPayload,
            resultPayload = resultPayload
        )

        return if (valid) {
            ValidationResult(ValidationStatus.ACCEPTED)
        } else {
            ValidationResult(ValidationStatus.REJECTED, "Hash invalid")
        }
    }
}

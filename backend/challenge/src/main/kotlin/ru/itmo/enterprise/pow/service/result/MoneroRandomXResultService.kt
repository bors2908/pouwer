package ru.itmo.enterprise.pow.service.result

import org.springframework.stereotype.Service
import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.MoneroRandomXResultPayload
import ru.itmo.enterprise.pow.model.MoneroRandomXTaskPayload
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.model.ValidationResult
import ru.itmo.enterprise.pow.service.PowValidator
import ru.itmo.enterprise.pow.service.TaskStore

@Service
class MoneroRandomXResultService(
    taskStore: TaskStore,
    private val validator: PowValidator<MoneroRandomXTaskPayload, MoneroRandomXResultPayload>
) : BaseResultService<MoneroRandomXTaskPayload, MoneroRandomXResultPayload>(
    taskStore = taskStore,
    taskPayloadClass = MoneroRandomXTaskPayload::class,
    resultPayloadClass = MoneroRandomXResultPayload::class
) {
    override val type: JobType = JobType.MONERO_RANDOMX

    override fun validate(
        task: Task,
        taskPayload: MoneroRandomXTaskPayload,
        resultPayload: MoneroRandomXResultPayload
    ): ValidationResult {
        val valid = validator.verify(taskPayload, resultPayload)

        return if (!valid) {
            rejected("Hash invalid")
        } else {
            accepted()
        }
    }
}

package ru.itmo.enterprise.pow.service.result

import ru.itmo.enterprise.pow.model.ResultMessage
import ru.itmo.enterprise.pow.model.ResultPayload
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.model.TaskPayload
import ru.itmo.enterprise.pow.model.ValidationResult
import ru.itmo.enterprise.pow.model.ValidationStatus
import ru.itmo.enterprise.pow.service.ResultService
import ru.itmo.enterprise.pow.service.TaskStore
import kotlin.reflect.KClass

abstract class BaseResultService<TTask : TaskPayload, TResult : ResultPayload>(
    private val taskStore: TaskStore,
    private val taskPayloadClass: KClass<TTask>,
    private val resultPayloadClass: KClass<TResult>
) : ResultService {

    final override fun handleResult(result: ResultMessage): ValidationResult {
        val task = taskStore.find(result.jobId)
            ?: return conflict("Unknown job")

        if (task.jobType != type || result.payload.jobType != type) {
            return rejected("JobType mismatch")
        }

        if (!taskPayloadClass.isInstance(task.payload)) {
            return rejected("Invalid task payload type")
        }
        if (!resultPayloadClass.isInstance(result.payload)) {
            return rejected("Invalid payload type")
        }

        @Suppress("UNCHECKED_CAST")
        val typedTaskPayload = task.payload as TTask
        @Suppress("UNCHECKED_CAST")
        val typedResultPayload = result.payload as TResult

        return validate(task, typedTaskPayload, typedResultPayload)
    }

    protected abstract fun validate(
        task: Task,
        taskPayload: TTask,
        resultPayload: TResult
    ): ValidationResult

    protected fun accepted(): ValidationResult = ValidationResult(ValidationStatus.ACCEPTED)

    protected fun rejected(reason: String): ValidationResult =
        ValidationResult(ValidationStatus.REJECTED, reason)

    protected fun conflict(reason: String): ValidationResult =
        ValidationResult(ValidationStatus.CONFLICT, reason)
}

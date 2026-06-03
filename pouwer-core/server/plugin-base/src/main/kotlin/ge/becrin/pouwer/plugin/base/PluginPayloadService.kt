package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult

interface PluginPayloadService {
    val pluginId: String

    fun createTask(workerId: String?, nowMillis: Long, taskTtlMillis: Long): Task

    fun validate(task: Task, result: ResultMessage): ValidationResult

    fun buildPayload(request: PayloadBuildRequest): Task =
        createTask(request.workerId, request.nowMillis, request.taskTtlMillis)
}

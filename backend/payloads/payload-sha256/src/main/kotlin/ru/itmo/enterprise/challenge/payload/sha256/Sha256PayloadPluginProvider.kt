package ru.itmo.enterprise.challenge.payload.sha256

import ru.itmo.enterprise.challenge.api.PayloadBuildRequest
import ru.itmo.enterprise.challenge.api.PayloadPlugin
import ru.itmo.enterprise.challenge.api.PayloadSupportContext
import ru.itmo.enterprise.challenge.api.ResultMessage
import ru.itmo.enterprise.challenge.api.Task
import ru.itmo.enterprise.challenge.api.ValidationResult

class Sha256PayloadPluginProvider : PayloadPlugin {
    private val delegate = Sha256PayloadPlugin()

    override fun id(): String = delegate.pluginId

    override fun version(): String = "1.0.0"

    override fun supports(context: PayloadSupportContext): Boolean {
        return context.requestedPluginId?.let { it == id() } ?: true
    }

    override fun buildPayload(request: PayloadBuildRequest): Task {
        return delegate.createTask(
            workerId = request.workerId,
            nowMillis = request.nowMillis,
            taskTtlMillis = request.taskTtlMillis
        )
    }

    override fun validateResult(task: Task, result: ResultMessage): ValidationResult {
        return delegate.validate(task, result)
    }
}

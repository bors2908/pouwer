package ru.itmo.enterprise.challenge.payload.bitcoin

import org.pf4j.Extension
import ru.itmo.enterprise.challenge.api.PayloadBuildRequest
import ru.itmo.enterprise.challenge.api.PayloadPlugin
import ru.itmo.enterprise.challenge.api.PayloadSupportContext
import ru.itmo.enterprise.challenge.api.ResultMessage
import ru.itmo.enterprise.challenge.api.Task
import ru.itmo.enterprise.challenge.api.ValidationResult

@Extension
class BitcoinRpcPayloadPluginProvider : PayloadPlugin {
    private val delegate = BitcoinRpcPayloadPlugin()

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

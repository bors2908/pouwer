package ge.becrin.pouwer.challenge.payload.bitcoin

import org.pf4j.Extension
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult

@Extension
class BitcoinRpcPayloadPluginProvider : PayloadPlugin {
    private val delegate = BitcoinRpcPayloadPlugin()

    override fun id(): String = delegate.pluginId

    override fun version(): String = "0.1.1"

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

package ge.becrin.pouwer.challenge.payload.monero

import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import org.pf4j.Extension

@Extension
class MoneroRandomXPayloadPluginProvider : PayloadPlugin {
    val jobStore: StratumJobStore = StratumJobStore()
    val converter: StratumToMoneroConverter = StratumToMoneroConverter()

    private val delegate = MoneroRandomXPayloadPlugin(
        jobStore = jobStore,
        converter = converter,
        stratumSubmitService = StratumSubmitService(
            client = MoneroStratumTcpClient(
                jobStore = jobStore
            )
        )
    )

    override fun id(): String = delegate.pluginId

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

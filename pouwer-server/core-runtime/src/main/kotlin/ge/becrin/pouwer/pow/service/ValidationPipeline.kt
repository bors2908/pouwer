package ge.becrin.pouwer.pow.service

import org.springframework.stereotype.Service
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus

@Service
class ValidationPipeline(
    private val taskStore: TaskStore,
    private val payloadPluginRegistry: PayloadPluginRegistry
) {
    fun validate(result: ResultMessage): ValidationResult {
        val task = taskStore.find(result.jobId)
            ?: return ValidationResult(ValidationStatus.CONFLICT, "Unknown job")

        val pluginId = result.pluginId ?: task.pluginId
        val plugin = payloadPluginRegistry.find(pluginId)
            ?: return ValidationResult(ValidationStatus.REJECTED, "Unsupported plugin: $pluginId")

        return try {
            plugin.validateResult(task, result)
        } catch (e: Exception) {
            payloadPluginRegistry.disable(pluginId, e)
            ValidationResult(ValidationStatus.REJECTED, "Plugin $pluginId failed and was disabled")
        }
    }
}

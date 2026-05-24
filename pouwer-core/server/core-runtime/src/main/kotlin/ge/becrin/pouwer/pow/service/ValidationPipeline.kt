package ge.becrin.pouwer.pow.service

import org.springframework.stereotype.Service
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus

@Service
class ValidationPipeline(
    private val taskStore: TaskStore,
    private val payloadPluginRegistry: PayloadPluginRegistry,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    fun validate(result: ResultMessage): ValidationResult {
        val task = taskStore.find(result.jobId)
            ?: return ValidationResult(ValidationStatus.CONFLICT, "Unknown job")

        if (task.expiresAt < currentTimeMillis()) {
            taskStore.remove(task.jobId)
            return ValidationResult(ValidationStatus.REJECTED, "Task expired")
        }

        if (result.pluginId != null && result.pluginId != task.pluginId) {
            return ValidationResult(ValidationStatus.REJECTED, "Plugin mismatch")
        }

        val pluginId = task.pluginId
        val plugin = payloadPluginRegistry.find(pluginId)
            ?: return ValidationResult(ValidationStatus.REJECTED, "Unsupported plugin: $pluginId")

        return try {
            val validation = plugin.validateResult(task, result)
            if (validation.status == ValidationStatus.ACCEPTED) {
                taskStore.remove(task.jobId)
            }
            validation
        } catch (e: Exception) {
            ValidationResult(ValidationStatus.REJECTED, "Plugin $pluginId failed: ${e.message}")
        }
    }
}

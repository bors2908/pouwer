package ru.itmo.enterprise.pow.service

import org.springframework.stereotype.Service
import ru.itmo.enterprise.challenge.api.ResultMessage
import ru.itmo.enterprise.challenge.api.ValidationResult
import ru.itmo.enterprise.challenge.api.ValidationStatus

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

        return plugin.validate(task, result)
    }
}

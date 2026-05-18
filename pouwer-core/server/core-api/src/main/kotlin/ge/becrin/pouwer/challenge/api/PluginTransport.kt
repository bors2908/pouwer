package ge.becrin.pouwer.challenge.api

interface PluginTransport {
    suspend fun buildPayload(plugin: PluginMetadata, request: PayloadBuildRequest): Task

    suspend fun validateResult(plugin: PluginMetadata, task: Task, result: ResultMessage): ValidationResult

    suspend fun supports(plugin: PluginMetadata, context: PayloadSupportContext): Boolean

    suspend fun health(plugin: PluginMetadata): Boolean
}

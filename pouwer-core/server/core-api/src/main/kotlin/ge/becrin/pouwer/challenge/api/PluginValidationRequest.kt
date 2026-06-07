package ge.becrin.pouwer.challenge.api

data class PluginValidationRequest(
    val task: Task,
    val result: ResultMessage
)
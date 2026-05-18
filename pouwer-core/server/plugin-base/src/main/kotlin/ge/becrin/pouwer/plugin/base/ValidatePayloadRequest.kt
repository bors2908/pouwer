package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task

data class ValidatePayloadRequest(
    val task: Task,
    val result: ResultMessage
)

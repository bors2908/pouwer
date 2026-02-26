package ru.itmo.enterprise.pow.service

import ru.itmo.enterprise.pow.model.JobType
import ru.itmo.enterprise.pow.model.ResultMessage
import ru.itmo.enterprise.pow.model.ValidationResult

interface ResultService {
    val type: JobType

    fun handleResult(result: ResultMessage): ValidationResult
}

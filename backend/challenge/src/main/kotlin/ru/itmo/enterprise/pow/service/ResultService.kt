package ru.itmo.enterprise.pow.service

import ru.itmo.enterprise.pow.model.ResultMessage
import ru.itmo.enterprise.pow.model.ValidationResult

interface ResultService {
    fun handleResult(result: ResultMessage): ValidationResult
}

package ru.itmo.enterprise.pow.service

import ru.itmo.enterprise.pow.model.ResultPayload
import ru.itmo.enterprise.pow.model.TaskPayload

interface PowValidator<T : TaskPayload, R : ResultPayload> {
    fun verify(taskPayload: T, resultPayload: R): Boolean
}

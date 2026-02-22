package ru.itmo.enterprise.pow.service

import ru.itmo.enterprise.pow.model.Task

interface LeaseManager {
    fun isNonceAllowed(task: Task, nonce: Long): Boolean
}

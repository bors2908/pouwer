package ru.itmo.enterprise.pow.service.lease

import ru.itmo.enterprise.pow.model.Sha256PowTaskPayload
import ru.itmo.enterprise.pow.model.Task
import ru.itmo.enterprise.pow.service.LeaseManager

class SimpleLeaseManager : LeaseManager {
    override fun isNonceAllowed(task: Task, nonce: Long): Boolean {
        val payload = task.payload as? Sha256PowTaskPayload ?: return false
        val range = payload.nonceRange
        return nonce >= range.start && nonce < range.end
    }
}

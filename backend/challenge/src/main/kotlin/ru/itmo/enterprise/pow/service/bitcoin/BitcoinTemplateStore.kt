package ru.itmo.enterprise.pow.service.bitcoin

import ru.itmo.enterprise.pow.client.bitcoin.BitcoinBlockTemplate
import java.util.UUID

interface BitcoinTemplateStore {
    fun save(jobId: UUID, template: BitcoinBlockTemplate, expiresAt: Long)
    fun find(jobId: UUID): BitcoinBlockTemplate?
}

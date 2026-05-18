package ge.becrin.pouwer.challenge.payload.bitcoin

import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface BitcoinTemplateStore {
    fun save(jobId: UUID, template: BitcoinBlockTemplate, expiresAt: Long)
    fun find(jobId: UUID): BitcoinBlockTemplate?
}

@Component
class InMemoryBitcoinTemplateStore : BitcoinTemplateStore {
    private val templates = ConcurrentHashMap<UUID, StoredTemplate>()

    override fun save(jobId: UUID, template: BitcoinBlockTemplate, expiresAt: Long) {
        cleanupExpired()
        templates[jobId] = StoredTemplate(template, expiresAt)
    }

    override fun find(jobId: UUID): BitcoinBlockTemplate? {
        val now = System.currentTimeMillis()
        val entry = templates[jobId] ?: return null
        if (entry.expiresAt <= now) {
            templates.remove(jobId)
            return null
        }
        return entry.template
    }

    private fun cleanupExpired() {
        val now = System.currentTimeMillis()
        templates.entries.removeIf { it.value.expiresAt <= now }
    }

    private data class StoredTemplate(
        val template: BitcoinBlockTemplate,
        val expiresAt: Long
    )
}

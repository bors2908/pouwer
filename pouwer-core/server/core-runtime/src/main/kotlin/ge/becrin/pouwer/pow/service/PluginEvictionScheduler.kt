package ge.becrin.pouwer.pow.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PluginEvictionScheduler(
    private val registry: RemotePluginRegistry
) {
    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    fun evictStalePlugins() {
        try {
            val evicted = registry.evictStale()
            if (evicted > 0) {
                log.info { "Eviction cycle: removed $evicted stale plugins" }
            }
        } catch (e: Exception) {
            log.error(e) { "Error during plugin eviction" }
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

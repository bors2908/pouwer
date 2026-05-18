package ge.becrin.pouwer.pow.controller

import ge.becrin.pouwer.challenge.api.PluginHeartbeat
import ge.becrin.pouwer.challenge.api.PluginRegistration
import ge.becrin.pouwer.pow.service.RemotePluginRegistry
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/core/plugins")
class PluginManagementController(
    private val registry: RemotePluginRegistry
) {
    @PostMapping("/register")
    fun register(@RequestBody registration: PluginRegistration): ResponseEntity<Map<String, String>> {
        return try {
            val metadata = registration.let {
                ge.becrin.pouwer.challenge.api.PluginMetadata(
                    id = it.id,
                    version = it.version,
                    contractVersion = it.contractVersion,
                    baseUrl = it.baseUrl,
                    lastHeartbeat = Instant.now(),
                    registeredAt = Instant.now()
                )
            }
            registry.register(metadata)
            ResponseEntity.ok(
                mapOf(
                    "status" to "registered",
                    "pluginId" to registration.id
                )
            )
        } catch (e: Exception) {
            log.error(e) { "Registration failed for plugin ${registration.id}" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    @PostMapping("/{pluginId}/heartbeat")
    fun heartbeat(
        @PathVariable pluginId: String,
        @RequestBody heartbeat: PluginHeartbeat
    ): ResponseEntity<Map<String, String>> {
        return try {
            val success = registry.heartbeat(pluginId, heartbeat.timestamp)
            if (success) {
                ResponseEntity.ok(
                    mapOf(
                        "status" to "accepted",
                        "pluginId" to pluginId
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Plugin not found: $pluginId"))
            }
        } catch (e: Exception) {
            log.error(e) { "Heartbeat failed for plugin $pluginId" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    @DeleteMapping("/{pluginId}")
    fun unregister(@PathVariable pluginId: String): ResponseEntity<Map<String, String>> {
        return try {
            val removed = registry.unregister(pluginId)
            if (removed) {
                ResponseEntity.ok(
                    mapOf(
                        "status" to "unregistered",
                        "pluginId" to pluginId
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Plugin not found: $pluginId"))
            }
        } catch (e: Exception) {
            log.error(e) { "Unregistration failed for plugin $pluginId" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginTransport
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import io.github.oshai.kotlinlogging.KotlinLogging

class RemotePayloadPluginAdapter(
    private val metadata: PluginMetadata,
    private val transport: PluginTransport
) : PayloadPlugin {
    override fun id(): String = metadata.id

    override fun version(): String = metadata.version

    override val contractVersion: String
        get() = metadata.contractVersion

    //TODO Do we need this?
    override fun supports(context: PayloadSupportContext): Boolean {
        return try {
            // Convert to blocking call
            val result = runBlocking {
                transport.supports(metadata, context)
            }
            result
        } catch (e: Exception) {
            log.warn(e) { "Error checking plugin support: ${metadata.id}" }
            false
        }
    }

    override fun buildPayload(request: PayloadBuildRequest): Task {
        return try {
            runBlocking {
                transport.buildPayload(metadata, request)
            }
        } catch (e: Exception) {
            log.error(e) { "Error building payload from plugin ${metadata.id}" }
            throw PluginTransportException("Failed to build payload", e)
        }
    }

    override fun validateResult(task: Task, result: ResultMessage): ValidationResult {
        return try {
            runBlocking {
                transport.validateResult(metadata, task, result)
            }
        } catch (e: Exception) {
            log.error(e) { "Error validating result on plugin ${metadata.id}" }
            throw PluginTransportException("Failed to validate result", e)
        }
    }

    private fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking {
            block()
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

class RemotePayloadPluginProvider(
    private val registry: RemotePluginRegistry,
    private val transport: PluginTransport
) : PayloadPluginProvider {
    override fun loadPlugins(): List<PayloadPlugin> {
        val plugins = registry.getHealthy()
        return plugins.map { metadata ->
            RemotePayloadPluginAdapter(metadata, transport)
        }.also {
            log.debug { "(Re-)Loaded ${it.size} remote plugins" }
        }
    }

    override fun disable(pluginId: String) {
        registry.markUnhealthy(pluginId)
        log.warn { "Disabled plugin $pluginId" }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

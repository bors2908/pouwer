package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginTransport
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CircuitBreakerPluginTransport(
    private val delegate: PluginTransport,
    private val lifecycle: CorePluginLifecycle,
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) : PluginTransport {

    override suspend fun buildPayload(plugin: PluginMetadata, request: PayloadBuildRequest): Task {
        return executeWithCircuitBreaker(plugin, "buildPayload") {
            delegate.buildPayload(plugin, request)
        }
    }

    override suspend fun validateResult(plugin: PluginMetadata, task: Task, result: ResultMessage): ValidationResult {
        return executeWithCircuitBreaker(plugin, "validateResult") {
            delegate.validateResult(plugin, task, result)
        }
    }

    override suspend fun health(plugin: PluginMetadata): Boolean {
        return try {
            executeWithCircuitBreaker(plugin, "health") {
                delegate.health(plugin)
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun <T> executeWithCircuitBreaker(
        plugin: PluginMetadata,
        operation: String,
        block: suspend () -> T
    ): T {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("pluginTransport")
        return try {
            withContext(Dispatchers.IO) {
                circuitBreaker.executeCallable {
                    kotlinx.coroutines.runBlocking {
                        block()
                    }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Circuit breaker failure for $operation on plugin ${plugin.id}" }
            lifecycle.markUnavailable(plugin.id, e)
            throw PluginTransportException("Circuit breaker open for ${plugin.id}: ${e.message}", e)
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

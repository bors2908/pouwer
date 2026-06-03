package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PluginMetadata
import ge.becrin.pouwer.challenge.api.PluginTransport
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.ValidationResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForObject

data class ValidateResultRequest(
    val task: Task,
    val result: ResultMessage
)

class RestPluginTransport(
    private val httpClient: RestTemplate = RestTemplate()
) : PluginTransport {

    override suspend fun buildPayload(plugin: PluginMetadata, request: PayloadBuildRequest): Task {
        return try {
            val url = "${plugin.baseUrl}/plugin/payload/build"

            httpClient.postForObject<Task>(url, request) ?: throw RuntimeException("Null response from plugin ${plugin.id}")
        } catch (e: Exception) {
            log.error(e) { "Failed to build payload on plugin ${plugin.id}" }
            throw PluginTransportException("Failed to build payload on ${plugin.id}", e)
        }
    }

    override suspend fun validateResult(plugin: PluginMetadata, task: Task, result: ResultMessage): ValidationResult {
        return try {
            val url = "${plugin.baseUrl}/plugin/payload/validate"
            val payload = ValidateResultRequest(task, result)

            httpClient.postForObject<ValidationResult>(url, payload)
                ?: throw RuntimeException("Null response from plugin ${plugin.id}")
        } catch (e: Exception) {
            log.error(e) { "Failed to validate result on plugin ${plugin.id}" }
            throw PluginTransportException("Failed to validate result on ${plugin.id}", e)
        }
    }

    override suspend fun health(plugin: PluginMetadata): Boolean {
        return try {
            val url = "${plugin.baseUrl}/plugin/health"

            httpClient.getForObject<String>(url) != null
        } catch (e: Exception) {
            log.debug(e) { "Health check failed for plugin ${plugin.id}" }
            false
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

class PluginTransportException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

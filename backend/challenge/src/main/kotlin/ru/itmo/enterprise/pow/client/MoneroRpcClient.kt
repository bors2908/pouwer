package ru.itmo.enterprise.pow.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.math.BigInteger

@Deprecated("Unused")
@Component
class MoneroRpcClient(
    private val objectMapper: ObjectMapper,
    @Value("\${monero.rpc.url:http://localhost:28081}") private val baseUrl: String
) {
    private val restTemplate = RestTemplate()

    private val rpcEndpoint: String
        get() = if (baseUrl.endsWith("/")) "$baseUrl/json_rpc" else "$baseUrl/json_rpc"

    /**
     * Calls get_block_template on the Monero daemon.
     * Returns the result JsonNode for downstream parsing.
     */
    fun getBlockTemplate(walletAddress: String?, reserveSize: Int = 0): BlockTemplateResult {
        val params = mutableMapOf<String, Any>("reserve_size" to reserveSize)
        if (!walletAddress.isNullOrBlank()) params["wallet_address"] = walletAddress

        val payload = mapOf(
            "jsonrpc" to "2.0",
            "id" to "0",
            "method" to "get_block_template",
            "params" to params
        )

        val respText = restTemplate.postForObject(rpcEndpoint, payload, String::class.java)
            ?: throw RuntimeException("Empty response from Monero RPC at $rpcEndpoint")

        val root = objectMapper.readTree(respText)
        val result = root.get("result") ?: throw RuntimeException("Missing 'result' in Monero RPC response: $respText")

        // Extract fields we need
        val blocktemplateBlob = result.path("blocktemplate_blob").takeIf(JsonNode::isTextual)?.asText()
        val blockhashingBlob = result.path("blockhashing_blob").takeIf(JsonNode::isTextual)?.asText()
        // difficulty may be numeric or string; parse via text
        val difficultyNode = result.get("difficulty")
            ?: result.get("difficulty64") // fallback name if different
            ?: throw RuntimeException("Missing 'difficulty' in block template result: $result")

        val difficulty = try {
            // difficulty might be big - parse as BigInteger from text
            BigInteger(difficultyNode.asText())
        } catch (ex: Exception) {
            throw RuntimeException("Cannot parse difficulty: ${difficultyNode.asText()}", ex)
        }

        return BlockTemplateResult(
            blocktemplateBlob = blocktemplateBlob,
            blockhashingBlob = blockhashingBlob,
            difficulty = difficulty
        )
    }

    fun submitBlock(blockBlobHex: String): Boolean {
        val payload = mapOf(
            "jsonrpc" to "2.0",
            "id" to "0",
            "method" to "submit_block",
            "params" to listOf(blockBlobHex)
        )

        val respText = restTemplate.postForObject(rpcEndpoint, payload, String::class.java)
            ?: throw RuntimeException("Empty response from Monero RPC at $rpcEndpoint")

        val root = objectMapper.readTree(respText)

        if (root.has("error")) {
            return false
        }

        // success if no error field
        return true
    }
}

data class BlockTemplateResult(
    val blocktemplateBlob: String?,
    val blockhashingBlob: String?,
    val difficulty: BigInteger
)

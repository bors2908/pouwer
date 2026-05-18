package ge.becrin.pouwer.challenge.payload.bitcoin

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.Base64

@Component
class BitcoinRpcClient {
    private val restTemplate = RestTemplate()
    private val rpcUrl = "http://localhost:18443"
    private val rpcUser = "rpcuser"
    private val rpcPassword = "rpcpassword"

    fun getBlockTemplate(): BitcoinBlockTemplate {
        val request = createRpcRequest("getblocktemplate", listOf(mapOf("rules" to listOf("segwit"))))
        val response = restTemplate.postForObject(rpcUrl, request, Map::class.java)
        val result = response?.get("result") as? Map<*, *>
            ?: throw IllegalStateException("Invalid Bitcoin RPC response: missing result")

        val transactions = (result["transactions"] as? List<*>)
            ?.map { tx ->
                val txMap = tx as? Map<*, *>
                    ?: throw IllegalStateException("Invalid Bitcoin RPC response: malformed transaction")
                val data = txMap["data"] as? String
                    ?: throw IllegalStateException("Invalid Bitcoin RPC response: transaction data missing")
                BitcoinTemplateTransaction(data = data)
            }
            ?: emptyList()

        return BitcoinBlockTemplate(
            version = (result["version"] as? Number)?.toLong()
                ?: throw IllegalStateException("Invalid Bitcoin RPC response: version missing"),
            previousBlockHash = result["previousblockhash"] as? String
                ?: throw IllegalStateException("Invalid Bitcoin RPC response: previousblockhash missing"),
            bits = result["bits"] as? String
                ?: throw IllegalStateException("Invalid Bitcoin RPC response: bits missing"),
            curTime = (result["curtime"] as? Number)?.toLong()
                ?: throw IllegalStateException("Invalid Bitcoin RPC response: curtime missing"),
            height = (result["height"] as? Number)?.toLong()
                ?: throw IllegalStateException("Invalid Bitcoin RPC response: height missing"),
            coinbaseValue = (result["coinbasevalue"] as? Number)?.toLong()
                ?: throw IllegalStateException("Invalid Bitcoin RPC response: coinbasevalue missing"),
            transactions = transactions
        )
    }

    fun submitBlock(blockHex: String): String? {
        val request = createRpcRequest("submitblock", listOf(blockHex))
        val response = restTemplate.postForObject(rpcUrl, request, Map::class.java)
        return response?.get("result") as? String
    }

    private fun createRpcRequest(method: String, params: List<Any>): HttpEntity<Map<String, Any>> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val auth = "$rpcUser:$rpcPassword"
        val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
        headers.set("Authorization", "Basic $encodedAuth")

        val body = mapOf(
            "jsonrpc" to "1.0",
            "id" to "pouwer-firewall",
            "method" to method,
            "params" to params
        )
        return HttpEntity(body, headers)
    }
}

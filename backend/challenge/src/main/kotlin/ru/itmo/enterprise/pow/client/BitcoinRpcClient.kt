package ru.itmo.enterprise.pow.client

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.Base64

@Service
class BitcoinRpcClient {
    private val restTemplate = RestTemplate()
    private val rpcUrl = "http://localhost:18443"
    private val rpcUser = "rpcuser"
    private val rpcPassword = "rpcpassword"

    fun getBlockTemplate(): Map<String, Any> {
        val request = createRpcRequest("getblocktemplate", listOf(mapOf("rules" to listOf("segwit"))))
        val response = restTemplate.postForObject(rpcUrl, request, Map::class.java)
        return response?.get("result") as Map<String, Any>
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
            "id" to "pouw-firewall",
            "method" to method,
            "params" to params
        )
        return HttpEntity(body, headers)
    }
}

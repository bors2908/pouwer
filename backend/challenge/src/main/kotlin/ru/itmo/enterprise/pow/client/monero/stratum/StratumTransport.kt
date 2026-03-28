package ru.itmo.enterprise.pow.client.monero.stratum

import ge.becrin.kt.stratum.message.ResponseMessage
import java.util.concurrent.CompletableFuture

/**
 * Minimal transport abstraction for sending JSON-RPC requests to the pool.
 * Implement this using JStratum's request API or (for PoC) a short-lived tcp socket request.
 *
 * sendRequest returns a future that resolves to the response JsonNode (or completesExceptionally).
 */
interface StratumTransport {
    fun sendRequest(method: String, params: List<Any>): CompletableFuture<ResponseMessage>
}

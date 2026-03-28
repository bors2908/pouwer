package ru.itmo.enterprise.pow.client.monero.stratum

import ge.becrin.kt.stratum.message.RequestMessage
import ge.becrin.kt.stratum.transport.AbstractConnectionState
import ge.becrin.kt.stratum.transport.tcp.StratumTcpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class MoneroStratumTcpClient(
    @param:Value("\${stratum.host:127.0.0.1}") private val host: String,
    @param:Value("\${stratum.port:3333}") private val port: Int,
    @param:Value("\${stratum.worker:worker}") private val worker: String,
    @param:Value("\${stratum.password:}") private val password: String
) : StratumTcpClient() {
    init {
        connect(host, port)
    }

    override fun createPostConnectState(): AbstractConnectionState? {
        return object : AbstractConnectionState(this) {
            override fun start() {
                sendRequest(RequestMessage(null, "mining.subscribe", listOf("jstratum-client")))
                sendRequest(RequestMessage(null, "mining.authorize", listOf(worker, password)))
            }
        }
    }
}

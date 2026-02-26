package ru.itmo.enterprise.pow.client.monero.stratum

import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage
import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class JStratumTransport(
    private val jStratumClient: MoneroStratumTcpClient,
    private val adapter: JStratumListenerAdapter
) : StratumTransport {
    //TODO Reuse internal holder/cache
    private val pending = ConcurrentHashMap<Int, CompletableFuture<ResponseMessage>>()
    private val idCounter = AtomicInteger(1)

    init {
        jStratumClient.registerResponseListener {
            if (it.id != null) {
                val id = it.id.toInt()

                val f = pending.remove(id)

                f?.complete(it)
            }
        }

        jStratumClient.registerRequestListener {
            adapter.onNotification(it.id, it.params)
        }
    }

    override fun sendRequest(method: String, params: List<Any>): CompletableFuture<ResponseMessage> {
        val id = idCounter.getAndIncrement()
        val future = CompletableFuture<ResponseMessage>()
        pending[id] = future

        jStratumClient.sendRequest(
            RequestMessage(id.toString(), method, params)
        )

        return future
    }
}

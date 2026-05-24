package ge.becrin.pouwer.challenge.payload.bitcoin

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class BitcoinRpcClientTest {
    private var server: HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
        server = null
    }

    @Test
    fun testGetBlockTemplateParsesValidResponse() {
        startRpcServer(
            """{"result":{"version":1,"previousblockhash":"${"00".repeat(32)}","bits":"207fffff","curtime":1700000000,"height":1,"coinbasevalue":10000,"transactions":[{"data":"00"}]}}"""
        )
        val client = BitcoinRpcClient()

        val template = client.getBlockTemplate()

        assertEquals(1, template.version)
        assertEquals("207fffff", template.bits)
        assertEquals(1, template.transactions.size)
    }

    @Test
    fun testGetBlockTemplateThrowsWhenResultMissing() {
        startRpcServer("""{"error":"bad"}""")
        val client = BitcoinRpcClient()

        assertThrows(IllegalStateException::class.java) {
            client.getBlockTemplate()
        }
    }

    @Test
    fun testSubmitBlockReturnsNullForNullResult() {
        val lastBody = AtomicReference<String>()
        startRpcServer("""{"result":null}""", lastBody)
        val client = BitcoinRpcClient()

        val result = client.submitBlock("deadbeef")

        assertNull(result)
        val body = lastBody.get()
        assertTrue(body.contains("submitblock"))
    }

    private fun startRpcServer(response: String, requestBodySink: AtomicReference<String>? = null) {
        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 18443), 0)
        httpServer.createContext("/") { exchange ->
            requestBodySink?.set(exchange.requestBody.readBytes().decodeToString())
            val bytes = response.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        httpServer.executor = Executors.newSingleThreadExecutor()
        httpServer.start()
        server = httpServer
    }
}

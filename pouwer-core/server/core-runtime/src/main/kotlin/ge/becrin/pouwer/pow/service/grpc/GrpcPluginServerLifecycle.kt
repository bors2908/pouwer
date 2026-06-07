package ge.becrin.pouwer.pow.service.grpc

import io.grpc.Server
import org.springframework.context.SmartLifecycle

class GrpcPluginServerLifecycle(
    private val server: Server
) : SmartLifecycle {
    @Volatile
    private var running = false

    override fun start() {
        server.start()
        running = true
    }

    override fun stop() {
        server.shutdown()
        running = false
    }

    override fun isRunning(): Boolean = running
}
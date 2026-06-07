package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.PluginHeartbeat
import ge.becrin.pouwer.challenge.api.PluginRegistration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PluginConnectivityCoordinatorTest {
    @Test
    fun `registers plugin on application ready through selected adapter`() {
        val adapter = RecordingAdapter()
        val coordinator = PluginConnectivityCoordinator(testDescriptor(), adapter)

        coordinator.onApplicationReady()

        assertTrue(adapter.started)
        val registration = adapter.registration
        assertNotNull(registration)
        assertEquals("plugin-1", registration!!.id)
        assertEquals("1.0.0", registration.version)
        assertEquals(CHALLENGE_PLUGIN_CONTRACT_VERSION, registration.contractVersion)
        assertEquals("http://localhost:8081", registration.baseUrl)
    }

    @Test
    fun `sends heartbeat through selected adapter`() {
        val adapter = RecordingAdapter()
        val coordinator = PluginConnectivityCoordinator(testDescriptor(), adapter)

        coordinator.sendHeartbeat()

        val heartbeat = adapter.heartbeat
        assertNotNull(heartbeat)
        assertEquals("plugin-1", heartbeat!!.id)
    }

    private fun testDescriptor(): PluginDescriptor = object : PluginDescriptor {
        override val pluginId: String = "plugin-1"
        override val version: String = "1.0.0"
        override val host: String = "http://localhost"
        override val port: Int = 8081
    }

    private class RecordingAdapter : PluginCoreTransportAdapter {
        var started = false
        var registration: PluginRegistration? = null
        var heartbeat: PluginHeartbeat? = null

        override fun start() {
            started = true
        }

        override fun register(registration: PluginRegistration) {
            this.registration = registration
        }

        override fun heartbeat(heartbeat: PluginHeartbeat) {
            this.heartbeat = heartbeat
        }
    }
}
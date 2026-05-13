package ge.becrin.pouwer.pow.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ServiceLoaderPluginProviderTest {

    @Test
    fun loadPlugins_returnsListEvenIfEmpty() {
        val provider = ServiceLoaderPluginProvider()
        val list = provider.loadPlugins()
        assertNotNull(list)
        assertTrue(list is List<*>)
    }
}

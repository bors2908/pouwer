package ge.becrin.pouwer.pow.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.pf4j.DefaultPluginManager
import org.pf4j.PluginWrapper
import org.pf4j.PluginDescriptor
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import java.nio.file.Files

class Pf4jPluginProviderValidateTest {

    @Test
    fun validateMetadata_success() {
        val dir = Files.createTempDirectory("pf4j")
        val provider = Pf4jPluginProvider(dir)

        val mockManager = mock<DefaultPluginManager>()
        val plugin = mock<PayloadPlugin>()
        whenever(plugin.id()).thenReturn("p1")
        whenever(plugin.version()).thenReturn("v1")
        whenever(plugin.contractVersion).thenReturn(CHALLENGE_PLUGIN_CONTRACT_VERSION)

        val descriptor = mock<PluginDescriptor>()
        whenever(descriptor.pluginId).thenReturn("p1")
        whenever(descriptor.version).thenReturn("v1")
        whenever(descriptor.requires).thenReturn(CHALLENGE_PLUGIN_CONTRACT_VERSION)

        val wrapper = mock<PluginWrapper>()
        whenever(wrapper.descriptor).thenReturn(descriptor)

        whenever(mockManager.unresolvedPlugins).thenReturn(emptyList())
        whenever(mockManager.getExtensions(PayloadPlugin::class.java)).thenReturn(listOf(plugin))
        whenever(mockManager.whichPlugin(plugin.javaClass)).thenReturn(wrapper)

        val field = Pf4jPluginProvider::class.java.getDeclaredField("pluginManager")
        field.isAccessible = true
        field.set(provider, mockManager)

        val res = provider.loadPlugins()
        assertEquals(1, res.size)
    }

    @Test
    fun validateMetadata_mismatch_throws() {
        val dir = Files.createTempDirectory("pf4j")
        val provider = Pf4jPluginProvider(dir)

        val mockManager = mock<DefaultPluginManager>()
        val plugin = mock<PayloadPlugin>()
        whenever(plugin.id()).thenReturn("p1")
        whenever(plugin.version()).thenReturn("v1")
        whenever(plugin.contractVersion).thenReturn(CHALLENGE_PLUGIN_CONTRACT_VERSION)

        val descriptor = mock<PluginDescriptor>()
        whenever(descriptor.pluginId).thenReturn("other")
        whenever(descriptor.version).thenReturn("v1")
        whenever(descriptor.requires).thenReturn(CHALLENGE_PLUGIN_CONTRACT_VERSION)

        val wrapper = mock<PluginWrapper>()
        whenever(wrapper.descriptor).thenReturn(descriptor)

        whenever(mockManager.unresolvedPlugins).thenReturn(emptyList())
        whenever(mockManager.getExtensions(PayloadPlugin::class.java)).thenReturn(listOf(plugin))
        whenever(mockManager.whichPlugin(plugin.javaClass)).thenReturn(wrapper)

        val field = Pf4jPluginProvider::class.java.getDeclaredField("pluginManager")
        field.isAccessible = true
        field.set(provider, mockManager)

        val ex = assertThrows(IllegalStateException::class.java) {
            provider.loadPlugins()
        }
        assertTrue(ex.message!!.contains("PF4J plugin id mismatch"))
    }
}

package ge.becrin.pouwer.pow.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.pf4j.DefaultPluginManager
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.pow.config.ChallengePluginsProperties
import java.nio.file.Files

class Pf4jPluginProviderTest {

    @Test
    fun loadPlugins_emptyAndDisableDestroy_invokesPluginManager() {
        val dir = Files.createTempDirectory("pf4j")
        val provider = Pf4jPluginProvider(ChallengePluginsProperties(dir.toString()))

        val mockManager = mock<DefaultPluginManager>()
        whenever(mockManager.unresolvedPlugins).thenReturn(emptyList())
        whenever(mockManager.getExtensions(PayloadPlugin::class.java)).thenReturn(emptyList())

        val field = Pf4jPluginProvider::class.java.getDeclaredField("pluginManager")
        field.isAccessible = true
        field.set(provider, mockManager)

        val res = provider.loadPlugins()
        assertTrue(res.isEmpty())

        verify(mockManager).loadPlugins()
        verify(mockManager).startPlugins()

        provider.disable("p1")
        verify(mockManager).disablePlugin("p1")
        verify(mockManager).stopPlugin("p1")
        verify(mockManager).unloadPlugin("p1")

        provider.destroy()
        verify(mockManager).stopPlugins()
        verify(mockManager).unloadPlugins()
    }
}

package ge.becrin.pouwer.pow.service

import org.pf4j.DefaultPluginManager
import org.pf4j.JarPluginLoader
import org.pf4j.PluginClassLoader
import org.pf4j.PluginDescriptor
import org.pf4j.ClassLoadingStrategy
import org.pf4j.CompoundPluginLoader
import org.springframework.beans.factory.DisposableBean
import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.pow.config.ChallengePluginsProperties
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class Pf4jPluginProvider(
    pluginsProperties: ChallengePluginsProperties
) : PayloadPluginProvider, DisposableBean {
    private val pluginsDirectory: Path = pluginsProperties.pluginDirectoryPath()

    private val pluginManager: org.pf4j.PluginManager = ParentFirstPluginManager(pluginsDirectory)

    init {
        pluginManager.setSystemVersion(CHALLENGE_PLUGIN_CONTRACT_VERSION)
    }

    override fun loadPlugins(): List<PayloadPlugin> {
        Files.createDirectories(pluginsDirectory)


        pluginManager.loadPlugins()
        val unresolved = pluginManager.unresolvedPlugins
        check(unresolved.isEmpty()) {
            "Unresolved PF4J plugins: ${unresolved.joinToString { it.pluginId }}"
        }
        pluginManager.startPlugins()

        return pluginManager.getExtensions(PayloadPlugin::class.java)
            .onEach { validatePf4jMetadata(it) }
    }

    override fun disable(pluginId: String) {
        pluginManager.disablePlugin(pluginId)
        pluginManager.stopPlugin(pluginId)
        pluginManager.unloadPlugin(pluginId)
    }

    override fun destroy() {
        pluginManager.stopPlugins()
        pluginManager.unloadPlugins()
    }

    private fun validatePf4jMetadata(plugin: PayloadPlugin) {
        val wrapper = requireNotNull(pluginManager.whichPlugin(plugin.javaClass)) {
            "PF4J extension ${plugin.javaClass.name} is not attached to any plugin descriptor"
        }
        val descriptor = wrapper.descriptor

        check(descriptor.pluginId == plugin.id()) {
            "PF4J plugin id mismatch: descriptor=${descriptor.pluginId}, spi=${plugin.id()}"
        }
        check(descriptor.version == plugin.version()) {
            "PF4J plugin version mismatch for ${plugin.id()}: descriptor=${descriptor.version}, spi=${plugin.version()}"
        }
        check(descriptor.requires == plugin.contractVersion) {
            "PF4J plugin requires mismatch for ${plugin.id()}: descriptor=${descriptor.requires}, spi=${plugin.contractVersion}"
        }
    }
}

private class ParentFirstPluginManager(pluginDir: Path) : DefaultPluginManager(pluginDir) {
    override fun createPluginLoader(): org.pf4j.PluginLoader {
        return CompoundPluginLoader()
            .add(ParentFirstJarPluginLoader(this), this::isNotDevelopment)
            .add(super.createPluginLoader(), { true })
    }
}

private class ParentFirstJarPluginLoader(pluginManager: org.pf4j.PluginManager) : JarPluginLoader(pluginManager) {
    override fun createPluginClassLoader(pluginPath: Path, pluginDescriptor: PluginDescriptor): PluginClassLoader {
        return PluginClassLoader(pluginManager, pluginDescriptor, javaClass.classLoader, ClassLoadingStrategy.APD)
    }
}

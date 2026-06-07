package ge.becrin.pouwer.plugin.base

abstract class BasePluginRegistration(
    final override val version: String
) : PluginDescriptor {
    abstract override val pluginId: String
    abstract override val port: Int
    abstract override val host: String
}

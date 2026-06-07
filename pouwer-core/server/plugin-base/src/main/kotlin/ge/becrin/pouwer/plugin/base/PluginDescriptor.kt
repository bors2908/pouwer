package ge.becrin.pouwer.plugin.base

interface PluginDescriptor {
    val pluginId: String
    val version: String
    val host: String
    val port: Int

    val baseUrl: String
        get() = "$host:$port"
}
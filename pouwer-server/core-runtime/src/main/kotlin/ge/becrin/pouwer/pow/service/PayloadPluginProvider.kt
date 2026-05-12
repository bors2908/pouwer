package ge.becrin.pouwer.pow.service

import ge.becrin.pouwer.challenge.api.PayloadPlugin

interface PayloadPluginProvider {
    fun loadPlugins(): List<PayloadPlugin>
    fun disable(pluginId: String)
}

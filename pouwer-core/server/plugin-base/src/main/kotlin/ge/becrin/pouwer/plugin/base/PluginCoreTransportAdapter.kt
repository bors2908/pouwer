package ge.becrin.pouwer.plugin.base

import ge.becrin.pouwer.challenge.api.PluginHeartbeat
import ge.becrin.pouwer.challenge.api.PluginRegistration

interface PluginCoreTransportAdapter {
    fun start() = Unit
    fun stop() = Unit
    fun register(registration: PluginRegistration)
    fun heartbeat(heartbeat: PluginHeartbeat)
    fun unregister(pluginId: String) = Unit
}
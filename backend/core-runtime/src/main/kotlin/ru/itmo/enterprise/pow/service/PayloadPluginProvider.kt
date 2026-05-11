package ru.itmo.enterprise.pow.service

import ru.itmo.enterprise.challenge.api.PayloadPlugin

interface PayloadPluginProvider {
    fun loadPlugins(): List<PayloadPlugin>
    fun disable(pluginId: String)
}

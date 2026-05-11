package ru.itmo.enterprise.pow.service

import ru.itmo.enterprise.challenge.api.PayloadPlugin
import java.util.ServiceLoader

class ServiceLoaderPluginProvider : PayloadPluginProvider {
    override fun loadPlugins(): List<PayloadPlugin> {
        return ServiceLoader.load(PayloadPlugin::class.java)
            .iterator()
            .asSequence()
            .toList()
    }

    override fun disable(pluginId: String) {
        // ServiceLoader plugins are removed at registry level.
    }
}

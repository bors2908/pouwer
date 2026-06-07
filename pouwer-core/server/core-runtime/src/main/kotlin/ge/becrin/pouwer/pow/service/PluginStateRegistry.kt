package ge.becrin.pouwer.pow.service

/**
 * Shared plugin-id keyed state holder for registry implementations.
 *
 * Transport-specific registries keep ownership of their domain rules, while this class keeps the
 * common mechanics of replacing, reading, listing, and conditionally removing plugin state in one place.
 */
internal class PluginStateRegistry<T> {
    private val byPluginId = linkedMapOf<String, T>()

    @Synchronized
    fun get(pluginId: String): T? = byPluginId[pluginId]

    @Synchronized
    fun pluginIds(): Set<String> = byPluginId.keys.toSet()

    @Synchronized
    fun replace(pluginId: String, state: T): T? = byPluginId.put(pluginId, state)

    @Synchronized
    fun replaceAll(states: Map<String, T>) {
        byPluginId.clear()
        byPluginId.putAll(states)
    }

    @Synchronized
    fun remove(pluginId: String): T? = byPluginId.remove(pluginId)

    @Synchronized
    fun removeIf(pluginId: String, predicate: (T) -> Boolean): Boolean {
        val state = byPluginId[pluginId] ?: return false
        return predicate(state).also { shouldRemove ->
            if (shouldRemove) {
                byPluginId.remove(pluginId)
            }
        }
    }

    @Synchronized
    fun updateIfPresent(pluginId: String, update: (T) -> T) {
        val state = byPluginId[pluginId] ?: return
        byPluginId[pluginId] = update(state)
    }
}
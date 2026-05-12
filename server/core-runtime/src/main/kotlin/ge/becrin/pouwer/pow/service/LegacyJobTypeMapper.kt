package ge.becrin.pouwer.pow.service

object LegacyJobTypeMapper {
    const val DEFAULT_PLUGIN_ID: String = "pow-test-sha256"

    private val legacyToPluginId = mapOf(
        "POW_TEST_SHA256" to "pow-test-sha256",
        "BITCOIN_RPC_SHA256" to "bitcoin-rpc-sha256",
        "MONERO_RANDOMX" to "monero-randomx"
    )

    fun resolve(jobType: String?, pluginId: String?): String {
        return when {
            !pluginId.isNullOrBlank() -> pluginId
            !jobType.isNullOrBlank() -> legacyToPluginId[jobType]
                ?: throw IllegalArgumentException("Unknown legacy job type: $jobType")
            else -> DEFAULT_PLUGIN_ID
        }
    }
}

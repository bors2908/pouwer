package ru.itmo.enterprise.pow.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LegacyJobTypeMapperTest {

    @Test
    fun `should prefer explicit plugin id`() {
        val resolved = LegacyJobTypeMapper.resolve(
            jobType = "POW_TEST_SHA256",
            pluginId = "custom-plugin"
        )

        assertEquals("custom-plugin", resolved)
    }

    @Test
    fun `should map legacy job type`() {
        val resolved = LegacyJobTypeMapper.resolve(
            jobType = "BITCOIN_RPC_SHA256",
            pluginId = null
        )

        assertEquals("bitcoin-rpc-sha256", resolved)
    }

    @Test
    fun `should use default plugin id when input is empty`() {
        val resolved = LegacyJobTypeMapper.resolve(
            jobType = null,
            pluginId = null
        )

        assertEquals(LegacyJobTypeMapper.DEFAULT_PLUGIN_ID, resolved)
    }

    @Test
    fun `should fail on unknown legacy job type`() {
        assertThrows(IllegalArgumentException::class.java) {
            LegacyJobTypeMapper.resolve(jobType = "UNKNOWN", pluginId = null)
        }
    }
}

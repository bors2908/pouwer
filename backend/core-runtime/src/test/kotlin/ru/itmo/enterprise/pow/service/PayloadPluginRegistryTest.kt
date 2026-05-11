package ru.itmo.enterprise.pow.service

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ru.itmo.enterprise.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ru.itmo.enterprise.challenge.api.ContractMismatchException
import ru.itmo.enterprise.challenge.api.DuplicatePluginIdException
import ru.itmo.enterprise.challenge.api.PayloadPlugin
import ru.itmo.enterprise.challenge.api.ResultMessage
import ru.itmo.enterprise.challenge.api.Task
import ru.itmo.enterprise.challenge.api.UnsupportedPluginException
import ru.itmo.enterprise.challenge.api.ValidationResult
import ru.itmo.enterprise.challenge.api.ValidationStatus
import java.util.UUID

class PayloadPluginRegistryTest {

    @Test
    fun `should reject duplicate plugin ids`() {
        val plugin = TestPlugin("dup")

        assertThrows(DuplicatePluginIdException::class.java) {
            PayloadPluginRegistry(listOf(plugin, plugin))
        }
    }

    @Test
    fun `should reject contract mismatch`() {
        val invalid = TestPlugin("invalid", contractVersion = "0.9")

        assertThrows(ContractMismatchException::class.java) {
            PayloadPluginRegistry(listOf(invalid))
        }
    }

    @Test
    fun `should expose plugins by id`() {
        val plugin = TestPlugin("ok")
        val registry = PayloadPluginRegistry(listOf(plugin))

        assertEquals(plugin, registry.get("ok"))
    }

    @Test
    fun `should handle empty plugin registry`() {
        val registry = PayloadPluginRegistry(emptyList())

        assertThrows(UnsupportedPluginException::class.java) {
            registry.get("pow-test-sha256")
        }
    }

    private data class TestPlugin(
        override val pluginId: String,
        override val contractVersion: String = CHALLENGE_PLUGIN_CONTRACT_VERSION
    ) : PayloadPlugin {
        override fun createTask(workerId: String?, nowMillis: Long, taskTtlMillis: Long): Task =
            Task(
                jobId = UUID.randomUUID(),
                pluginId = pluginId,
                expiresAt = nowMillis + taskTtlMillis,
                payload = JsonNodeFactory.instance.objectNode()
            )

        override fun validate(task: Task, result: ResultMessage): ValidationResult =
            ValidationResult(ValidationStatus.ACCEPTED)
    }
}

package ge.becrin.pouwer.pow.service

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ge.becrin.pouwer.challenge.api.CHALLENGE_PLUGIN_CONTRACT_VERSION
import ge.becrin.pouwer.challenge.api.ContractMismatchException
import ge.becrin.pouwer.challenge.api.DuplicatePluginIdException
import ge.becrin.pouwer.challenge.api.PayloadBuildRequest
import ge.becrin.pouwer.challenge.api.PayloadPlugin
import ge.becrin.pouwer.challenge.api.PayloadSupportContext
import ge.becrin.pouwer.challenge.api.ResultMessage
import ge.becrin.pouwer.challenge.api.Task
import ge.becrin.pouwer.challenge.api.UnsupportedPluginException
import ge.becrin.pouwer.challenge.api.ValidationResult
import ge.becrin.pouwer.challenge.api.ValidationStatus
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
    fun `should register plugins in deterministic order`() {
        val registry = PayloadPluginRegistry(listOf(TestPlugin("z"), TestPlugin("a"), TestPlugin("m")))

        assertEquals(listOf("a", "m", "z"), registry.pluginIds().toList())
    }

    @Test
    fun `should handle empty plugin registry`() {
        val registry = PayloadPluginRegistry(emptyList())

        assertThrows(UnsupportedPluginException::class.java) {
            registry.get("pow-test-sha256")
        }
    }

    @Test
    fun `should disable plugin`() {
        val registry = PayloadPluginRegistry(listOf(TestPlugin("ok")))
        registry.disable("ok")

        assertThrows(UnsupportedPluginException::class.java) {
            registry.get("ok")
        }
    }

    @Test
    fun `should call provider disable when plugin is disabled`() {
        var disabledId: String? = null
        val provider = object : PayloadPluginProvider {
            override fun loadPlugins(): List<PayloadPlugin> = listOf(TestPlugin("faulty"))
            override fun disable(pluginId: String) {
                disabledId = pluginId
            }
        }
        val registry = PayloadPluginRegistry(provider)
        
        registry.disable("faulty")
        
        assertEquals("faulty", disabledId)
        assertThrows(UnsupportedPluginException::class.java) {
            registry.get("faulty")
        }
    }

    private data class TestPlugin(
        private val pluginId: String,
        override val contractVersion: String = CHALLENGE_PLUGIN_CONTRACT_VERSION
    ) : PayloadPlugin {
        override fun id(): String = pluginId

        override fun version(): String = "test"

        override fun supports(context: PayloadSupportContext): Boolean = true

        override fun buildPayload(request: PayloadBuildRequest): Task =
            Task(
                jobId = UUID.randomUUID(),
                pluginId = pluginId,
                expiresAt = request.nowMillis + request.taskTtlMillis,
                payload = JsonNodeFactory.instance.objectNode()
            )

        override fun validateResult(task: Task, result: ResultMessage): ValidationResult =
            ValidationResult(ValidationStatus.ACCEPTED)
    }
}

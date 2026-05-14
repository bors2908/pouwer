package ge.becrin.pouwer.pow

import ge.becrin.pouwer.UnitTestBase
import ge.becrin.pouwer.challenge.api.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import java.util.UUID

class ContractsCoreRuntimeTest : UnitTestBase() {

    @Test
    fun constantAndEnum() {
        assertEquals("0.1.0", CHALLENGE_PLUGIN_CONTRACT_VERSION)
        assertEquals(ValidationStatus.ACCEPTED, ValidationStatus.valueOf("ACCEPTED"))
    }

    @Test
    fun payloadPluginDefaultContractVersion() {
        val plugin = object : PayloadPlugin {
            override fun id() = "test"
            override fun version() = "v"
            override fun supports(context: PayloadSupportContext) = true
            override fun buildPayload(request: PayloadBuildRequest): Task {
                return Task(UUID.randomUUID(), id(), request.nowMillis + 1000, JsonNodeFactory.instance.objectNode())
            }
            override fun validateResult(task: Task, result: ResultMessage): ValidationResult {
                return ValidationResult(ValidationStatus.ACCEPTED)
            }
        }
        assertEquals(CHALLENGE_PLUGIN_CONTRACT_VERSION, plugin.contractVersion)
    }

    @Test
    fun exceptionsMessages() {
        val dup = DuplicatePluginIdException("p1")
        assertTrue(dup.message!!.contains("Duplicate plugin id: p1"))

        val cm = ContractMismatchException("p1", "exp", "act")
        assertTrue(cm.message!!.contains("Plugin p1 contract mismatch. Expected exp, got act"))

        val us = UnsupportedPluginException("p1", setOf("a", "b"))
        assertTrue(us.message!!.contains("Unsupported plugin: p1"))
        assertTrue(us.message!!.contains("a") && us.message!!.contains("b"))
    }

    @Test
    fun dataClassesCoverage() {
        val ctx = PayloadSupportContext("pid", "worker")
        assertEquals("pid", ctx.requestedPluginId)

        val req = PayloadBuildRequest("worker1", 123L, 456L, "pid")
        val reqCopy = req.copy(taskTtlMillis = 999L)
        assertNotEquals(req, reqCopy)
        assertTrue(req.toString().contains("worker1"))
    }
}

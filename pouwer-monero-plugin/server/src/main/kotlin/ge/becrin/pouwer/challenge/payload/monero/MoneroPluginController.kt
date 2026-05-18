package ge.becrin.pouwer.challenge.payload.monero

import ge.becrin.pouwer.plugin.base.BasePluginController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/plugin")
class MoneroPluginController(
    payloadPlugin: MoneroRandomXPayloadPlugin
) : BasePluginController(payloadPlugin)

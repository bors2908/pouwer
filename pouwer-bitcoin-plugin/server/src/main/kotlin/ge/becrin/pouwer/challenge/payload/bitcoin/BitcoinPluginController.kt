package ge.becrin.pouwer.challenge.payload.bitcoin

import ge.becrin.pouwer.plugin.base.BasePluginController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/plugin")
class BitcoinPluginController(
    payloadPlugin: BitcoinRpcPayloadPlugin
) : BasePluginController(payloadPlugin)

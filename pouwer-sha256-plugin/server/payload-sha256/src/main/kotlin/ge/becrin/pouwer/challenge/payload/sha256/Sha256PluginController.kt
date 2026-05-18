package ge.becrin.pouwer.challenge.payload.sha256

import ge.becrin.pouwer.plugin.base.BasePluginController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/plugin")
class Sha256PluginController(
    payloadPlugin: Sha256PayloadPlugin
) : BasePluginController(payloadPlugin)

package ge.becrin.pouwer.challenge.payload.sha256

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["ge.becrin.pouwer"])
open class Sha256PluginApplication

fun main(args: Array<String>) {
    runApplication<Sha256PluginApplication>(*args)
}

package ge.becrin.pouwer.challenge.payload.monero

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["ge.becrin.pouwer"])
open class MoneroPluginApplication

fun main(args: Array<String>) {
    runApplication<MoneroPluginApplication>(*args)
}

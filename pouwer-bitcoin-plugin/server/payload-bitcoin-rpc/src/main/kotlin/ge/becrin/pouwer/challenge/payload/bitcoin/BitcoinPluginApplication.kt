package ge.becrin.pouwer.challenge.payload.bitcoin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["ge.becrin.pouwer"])
open class BitcoinPluginApplication

fun main(args: Array<String>) {
    runApplication<BitcoinPluginApplication>(*args)
}
